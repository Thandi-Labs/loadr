package com.bytethrux.loadr.service

import com.bytethrux.loadr.data.local.LoadrSettings
import com.bytethrux.loadr.data.local.ProcessingMode
import com.bytethrux.loadr.data.local.SubscriptionState
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.CreateTransactionRequest
import com.bytethrux.loadr.data.network.OfferDto
import com.bytethrux.loadr.data.network.OfferType
import com.bytethrux.loadr.data.repository.SubscriptionsRepository
import com.bytethrux.loadr.data.ussd.UssdExecutor
import com.bytethrux.loadr.data.ussd.UssdResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Mock-driven "mockups" of the full SMS → offer → USSD → DB pipeline,
 * proving each step behaves safely without touching the network or modem.
 */
class PaymentProcessorTest {

    private lateinit var api: ApiService
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var subscriptionStore: SubscriptionStore
    private lateinit var subscriptionsRepository: SubscriptionsRepository
    private lateinit var ussdExecutor: UssdExecutor
    private lateinit var processor: PaymentProcessor

    private val settings = LoadrSettings(
        bingwaSimSlot = 1,
        processingMode = ProcessingMode.EXPRESS,
    )

    private val activeEntitlement = SubscriptionState(
        expiryAt = System.currentTimeMillis() + 86_400_000L,
        tokens = 300,
    )

    private val offers = listOf(
        OfferDto(1, "1GB Daily", "*180*5*2*LD*1#", 20.0, true, OfferType.DATA),
        OfferDto(2, "2GB Weekly", "*180*5*3*LD*2#", 50.0, true, OfferType.DATA),
        OfferDto(3, "Retired 50", "*180*9*LD#", 50.0, false, OfferType.DATA),
    )

    @Before
    fun setUp() {
        api = mockk()
        tokenDataStore = mockk()
        subscriptionStore = mockk(relaxed = true)
        subscriptionsRepository = mockk()
        ussdExecutor = mockk()
        processor = PaymentProcessor(
            api, tokenDataStore, subscriptionStore, subscriptionsRepository, ussdExecutor
        )

        every { tokenDataStore.accessToken } returns flowOf("tkn")
        coEvery { subscriptionsRepository.syncMySubscription() } returns activeEntitlement
        coEvery { subscriptionsRepository.consumeTokenRemote() } returns true
        coEvery { api.getOffers(any()) } returns offers
        coEvery { api.createTransaction(any(), any()) } returns Unit
    }

    // ------------------------------------------------------------------
    // Happy path: SMS amount → offer → LD replacement → USSD → DB + token
    // ------------------------------------------------------------------

    @Test
    fun `matches offer by amount and replaces LD with the customer phone`() = runTest {
        val codeSlot = slot<String>()
        coEvery { ussdExecutor.run(capture(codeSlot), any(), any()) } returns UssdResult(true, "OK")

        val outcome = processor.process(50.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.Sent)
        assertEquals("2GB Weekly", (outcome as PaymentProcessor.Outcome.Sent).offer.offer_name)
        assertEquals("*180*5*3*0712345678*2#", codeSlot.captured)
    }

    @Test
    fun `runs the USSD on the configured Bingwa SIM slot`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)

        processor.process(20.0, "0712345678", "JOHN DOE", settings)

        coVerify { ussdExecutor.run(any(), simSlot = 1, multiStep = false) }
    }

    @Test
    fun `advanced mode runs the USSD as multi-step`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)

        processor.process(
            20.0, "0712345678", "JOHN DOE",
            settings.copy(processingMode = ProcessingMode.ADVANCED),
        )

        coVerify { ussdExecutor.run(any(), any(), multiStep = true) }
    }

    @Test
    fun `successful USSD stores the transaction in the DB with status success`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)
        val txSlot = slot<CreateTransactionRequest>()
        coEvery { api.createTransaction(any(), capture(txSlot)) } returns Unit

        processor.process(50.0, "0712345678", "JOHN DOE", settings)

        with(txSlot.captured) {
            assertEquals(2, offer_id)
            assertEquals("JOHN DOE", customer_name)
            assertEquals("0712345678", customer_phone)
            assertEquals(50, amount)
            assertEquals("success", status)
        }
    }

    @Test
    fun `successful USSD consumes exactly one token`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)

        processor.process(50.0, "0712345678", "JOHN DOE", settings)

        coVerify(exactly = 1) { subscriptionStore.consumeToken() }
    }

    @Test
    fun `successful USSD decrements the backend and refetches the remainder`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)

        processor.process(50.0, "0712345678", "JOHN DOE", settings)

        coVerify(exactly = 1) { subscriptionsRepository.consumeTokenRemote() }
        // Once for the entitlement gate, once to refetch after the decrement.
        coVerify(exactly = 2) { subscriptionsRepository.syncMySubscription() }
    }

    @Test
    fun `failed remote decrement skips the refetch but keeps the local decrement`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)
        coEvery { subscriptionsRepository.consumeTokenRemote() } returns false

        val outcome = processor.process(50.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.Sent)
        coVerify(exactly = 1) { subscriptionStore.consumeToken() }
        coVerify(exactly = 1) { subscriptionsRepository.syncMySubscription() } // gate only
    }

    @Test
    fun `onSending fires with the offer name before the USSD runs`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)
        var sending: String? = null

        processor.process(20.0, "0712345678", "JOHN DOE", settings) { sending = it }

        assertEquals("1GB Daily", sending)
    }

    // ------------------------------------------------------------------
    // Failure paths
    // ------------------------------------------------------------------

    @Test
    fun `failed USSD stores a failed transaction and consumes no token`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(false, "timeout")
        val txSlot = slot<CreateTransactionRequest>()
        coEvery { api.createTransaction(any(), capture(txSlot)) } returns Unit

        val outcome = processor.process(50.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.UssdFailed)
        assertEquals("failed", txSlot.captured.status)
        coVerify(exactly = 0) { subscriptionStore.consumeToken() }
        coVerify(exactly = 0) { subscriptionsRepository.consumeTokenRemote() }
    }

    @Test
    fun `no entitlement blocks the pipeline before any USSD or DB call`() = runTest {
        coEvery { subscriptionsRepository.syncMySubscription() } returns SubscriptionState()

        val outcome = processor.process(50.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.NoEntitlement)
        coVerify(exactly = 0) { ussdExecutor.run(any(), any(), any()) }
        coVerify(exactly = 0) { api.getOffers(any()) }
        coVerify(exactly = 0) { api.createTransaction(any(), any()) }
    }

    @Test
    fun `expired subscription with tokens left blocks the pipeline`() = runTest {
        coEvery { subscriptionsRepository.syncMySubscription() } returns SubscriptionState(
            expiryAt = System.currentTimeMillis() - 1_000, tokens = 100
        )

        assertTrue(
            processor.process(50.0, "0712345678", "JOHN DOE", settings)
                is PaymentProcessor.Outcome.NoEntitlement
        )
    }

    @Test
    fun `missing auth token stops before the USSD`() = runTest {
        every { tokenDataStore.accessToken } returns flowOf(null)

        val outcome = processor.process(50.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.NotSignedIn)
        coVerify(exactly = 0) { ussdExecutor.run(any(), any(), any()) }
    }

    @Test
    fun `unknown amount runs no USSD and stores nothing`() = runTest {
        val outcome = processor.process(35.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.NoMatchingOffer)
        coVerify(exactly = 0) { ussdExecutor.run(any(), any(), any()) }
        coVerify(exactly = 0) { api.createTransaction(any(), any()) }
        coVerify(exactly = 0) { subscriptionStore.consumeToken() }
    }

    @Test
    fun `inactive offers are never matched even when the amount fits`() = runTest {
        coEvery { api.getOffers(any()) } returns listOf(offers[2]) // Retired 50, inactive

        assertTrue(
            processor.process(50.0, "0712345678", "JOHN DOE", settings)
                is PaymentProcessor.Outcome.NoMatchingOffer
        )
    }

    @Test
    fun `offers endpoint failure aborts without running a USSD`() = runTest {
        coEvery { api.getOffers(any()) } throws RuntimeException("HTTP 500")

        val outcome = processor.process(50.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.OffersUnavailable)
        coVerify(exactly = 0) { ussdExecutor.run(any(), any(), any()) }
    }

    @Test
    fun `DB write failure does not change the outcome of an executed USSD`() = runTest {
        coEvery { ussdExecutor.run(any(), any(), any()) } returns UssdResult(true)
        coEvery { api.createTransaction(any(), any()) } throws RuntimeException("HTTP 503")

        val outcome = processor.process(50.0, "0712345678", "JOHN DOE", settings)

        assertTrue(outcome is PaymentProcessor.Outcome.Sent)
        coVerify(exactly = 1) { subscriptionStore.consumeToken() }
    }
}
