package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.OfferDto
import com.bytethrux.loadr.data.network.OfferType
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class OffersRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var repository: OffersRepository

    private val token = "test_token"
    private val bearer = "Bearer $token"

    private val offer = OfferDto(
        id = 1, offer_name = "250MB 24HRS", ussd = "*544*1*3#",
        amount = 20.0, active = true, category = OfferType.DATA
    )

    @Before
    fun setUp() {
        api = mockk()
        tokenDataStore = mockk()
        every { tokenDataStore.accessToken } returns flowOf(token)
        repository = OffersRepository(api, tokenDataStore)
    }

    // ------------------------------------------------------------------
    // getOffers()
    // ------------------------------------------------------------------

    @Test
    fun `getOffers success returns list`() = runTest {
        coEvery { api.getOffers(bearer) } returns listOf(offer)

        val result = repository.getOffers()

        assertTrue(result is OffersResult.Success)
        assertEquals(listOf(offer), (result as OffersResult.Success).data)
    }

    @Test
    fun `getOffers network error returns Error`() = runTest {
        coEvery { api.getOffers(any()) } throws Exception("Timeout")

        val result = repository.getOffers()

        assertTrue(result is OffersResult.Error)
        assertEquals("Timeout", (result as OffersResult.Error).message)
    }

    @Test
    fun `getOffers with null token returns not authenticated`() = runTest {
        every { tokenDataStore.accessToken } returns flowOf(null)

        val result = repository.getOffers()

        assertTrue(result is OffersResult.Error)
        assertEquals("Not authenticated", (result as OffersResult.Error).message)
    }

    @Test
    fun `getOffers sends correct bearer header`() = runTest {
        val captured = slot<String>()
        coEvery { api.getOffers(capture(captured)) } returns emptyList()

        repository.getOffers()

        assertEquals(bearer, captured.captured)
    }

    // ------------------------------------------------------------------
    // createOffer()
    // ------------------------------------------------------------------

    @Test
    fun `createOffer success returns Success`() = runTest {
        coEvery { api.createOffer(bearer, offer) } just Runs

        val result = repository.createOffer(offer)

        assertTrue(result is OffersResult.Success)
    }

    @Test
    fun `createOffer error returns Error with message`() = runTest {
        coEvery { api.createOffer(any(), any()) } throws Exception("Validation failed")

        val result = repository.createOffer(offer)

        assertTrue(result is OffersResult.Error)
        assertEquals("Validation failed", (result as OffersResult.Error).message)
    }

    // ------------------------------------------------------------------
    // updateOffer()
    // ------------------------------------------------------------------

    @Test
    fun `updateOffer success returns Success and passes correct id`() = runTest {
        val idSlot = slot<Int>()
        coEvery { api.updateOffer(bearer, capture(idSlot), offer) } just Runs

        val result = repository.updateOffer(offer)

        assertTrue(result is OffersResult.Success)
        assertEquals(offer.id, idSlot.captured)
    }

    @Test
    fun `updateOffer error returns Error`() = runTest {
        coEvery { api.updateOffer(any(), any(), any()) } throws Exception("Update failed")

        val result = repository.updateOffer(offer)

        assertTrue(result is OffersResult.Error)
        assertEquals("Update failed", (result as OffersResult.Error).message)
    }

    // ------------------------------------------------------------------
    // deleteOffer()
    // ------------------------------------------------------------------

    @Test
    fun `deleteOffer success returns Success`() = runTest {
        coEvery { api.deleteOffer(bearer, 1) } just Runs

        val result = repository.deleteOffer(1)

        assertTrue(result is OffersResult.Success)
    }

    @Test
    fun `deleteOffer error returns Error`() = runTest {
        coEvery { api.deleteOffer(any(), any()) } throws Exception("Delete failed")

        val result = repository.deleteOffer(1)

        assertTrue(result is OffersResult.Error)
        assertEquals("Delete failed", (result as OffersResult.Error).message)
    }

    // ------------------------------------------------------------------
    // activateOffer() / deactivateOffer()
    // ------------------------------------------------------------------

    @Test
    fun `activateOffer success returns Success`() = runTest {
        coEvery { api.activateOffer(bearer, 1) } just Runs

        val result = repository.activateOffer(1)

        assertTrue(result is OffersResult.Success)
    }

    @Test
    fun `activateOffer error returns Error`() = runTest {
        coEvery { api.activateOffer(any(), any()) } throws Exception("Activate failed")

        val result = repository.activateOffer(1)

        assertTrue(result is OffersResult.Error)
        assertEquals("Activate failed", (result as OffersResult.Error).message)
    }

    @Test
    fun `deactivateOffer success returns Success`() = runTest {
        coEvery { api.deactivateOffer(bearer, 1) } just Runs

        val result = repository.deactivateOffer(1)

        assertTrue(result is OffersResult.Success)
    }

    @Test
    fun `deactivateOffer error returns Error`() = runTest {
        coEvery { api.deactivateOffer(any(), any()) } throws Exception("Deactivate failed")

        val result = repository.deactivateOffer(1)

        assertTrue(result is OffersResult.Error)
        assertEquals("Deactivate failed", (result as OffersResult.Error).message)
    }
}
