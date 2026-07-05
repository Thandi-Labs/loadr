package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.SubscriptionState
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.SubscriptionPlanDto
import com.bytethrux.loadr.data.network.UserSubscriptionDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SubscriptionsRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var store: SubscriptionStore
    private lateinit var repository: SubscriptionsRepository

    private fun httpException(code: Int, body: String = "{}") = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType()))
    )

    @Before
    fun setUp() {
        api = mockk()
        tokenDataStore = mockk()
        store = mockk(relaxed = true)
        every { tokenDataStore.accessToken } returns flowOf("token-123")
        repository = SubscriptionsRepository(api, tokenDataStore, store)
    }

    // ------------------------------------------------------------------
    // parseExpiry()
    // ------------------------------------------------------------------

    @Test
    fun `parses FastAPI datetime with microseconds`() {
        assertNotNull(SubscriptionsRepository.parseExpiry("2026-08-04T07:52:11.123456"))
    }

    @Test
    fun `parses datetime without fraction`() {
        assertNotNull(SubscriptionsRepository.parseExpiry("2026-08-04T07:52:11"))
    }

    @Test
    fun `later expiry parses to a larger timestamp`() {
        val early = SubscriptionsRepository.parseExpiry("2026-08-04T07:52:11")!!
        val later = SubscriptionsRepository.parseExpiry("2026-09-04T07:52:11")!!
        assertTrue(later > early)
    }

    @Test
    fun `garbage expiry returns null`() {
        assertNull(SubscriptionsRepository.parseExpiry("not-a-date"))
    }

    // ------------------------------------------------------------------
    // getPlans()
    // ------------------------------------------------------------------

    @Test
    fun `getPlans returns only active plans`() = runTest {
        coEvery { api.getSubscriptionPlans() } returns listOf(
            SubscriptionPlanDto(1, "Daily", "Unlimited Daily", 1000, 30.0, 1, active = true),
            SubscriptionPlanDto(2, "Old", "Retired", 10, 5.0, 1, active = false),
        )

        val result = repository.getPlans() as HomeResult.Success
        assertEquals(listOf(1), result.data.map { it.id })
    }

    @Test
    fun `getPlans surfaces failures as Error`() = runTest {
        coEvery { api.getSubscriptionPlans() } throws RuntimeException("boom")
        assertTrue(repository.getPlans() is HomeResult.Error)
    }

    // ------------------------------------------------------------------
    // syncMySubscription()
    // ------------------------------------------------------------------

    @Test
    fun `sync mirrors the backend subscription into the store`() = runTest {
        coEvery { api.getMySubscription(any()) } returns UserSubscriptionDto(
            id = 1, user_id = 7, subscription_id = 2, requests_remaining = 280,
            start_date = "2026-07-05T08:00:00", expiry_date = "2026-08-04T08:00:00",
        )
        coEvery { store.current() } returns SubscriptionState(123L, 280)

        repository.syncMySubscription()

        coVerify { store.setFromBackend(any(), 280) }
    }

    @Test
    fun `404 clears the cached entitlement`() = runTest {
        coEvery { api.getMySubscription(any()) } throws httpException(404)
        coEvery { store.current() } returns SubscriptionState()

        repository.syncMySubscription()

        coVerify { store.clearEntitlement() }
    }

    @Test
    fun `network failure keeps the cached entitlement`() = runTest {
        coEvery { api.getMySubscription(any()) } throws RuntimeException("offline")
        val cached = SubscriptionState(expiryAt = 99L, tokens = 42)
        coEvery { store.current() } returns cached

        assertEquals(cached, repository.syncMySubscription())
        coVerify(exactly = 0) { store.clearEntitlement() }
        coVerify(exactly = 0) { store.setFromBackend(any(), any()) }
    }

    @Test
    fun `missing auth token returns cache without calling the API`() = runTest {
        every { tokenDataStore.accessToken } returns flowOf(null)
        val cached = SubscriptionState(expiryAt = 5L, tokens = 3)
        coEvery { store.current() } returns cached

        assertEquals(cached, repository.syncMySubscription())
        coVerify(exactly = 0) { api.getMySubscription(any()) }
    }

    // ------------------------------------------------------------------
    // purchase()
    // ------------------------------------------------------------------

    @Test
    fun `purchase success returns Success`() = runTest {
        coEvery { api.purchaseSubscription(any(), 3) } returns Unit
        assertTrue(repository.purchase(3) is HomeResult.Success)
    }

    @Test
    fun `purchase surfaces backend detail message`() = runTest {
        coEvery { api.purchaseSubscription(any(), 3) } throws httpException(
            400, """{"detail":"You already have an active subscription"}"""
        )

        val result = repository.purchase(3) as HomeResult.Error
        assertEquals("You already have an active subscription", result.message)
    }

    @Test
    fun `purchase without auth returns Error`() = runTest {
        every { tokenDataStore.accessToken } returns flowOf(null)
        val result = repository.purchase(3) as HomeResult.Error
        assertEquals("Not authenticated", result.message)
    }
}
