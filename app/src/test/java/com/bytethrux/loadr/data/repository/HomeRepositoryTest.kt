package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.TransactionDto
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HomeRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var tokenDataStore: TokenDataStore
    private lateinit var repository: HomeRepository

    @Before
    fun setUp() {
        api = mockk()
        tokenDataStore = mockk()
        every { tokenDataStore.accessToken } returns flowOf("test_token")
        repository = HomeRepository(api, tokenDataStore)
    }

    // ------------------------------------------------------------------
    // getStats() — always returns hardcoded mock, never hits the API
    // ------------------------------------------------------------------

    @Test
    fun `getStats returns Success without calling the API`() = runTest {
        val result = repository.getStats()

        assertTrue(result is HomeResult.Success)
        coVerify(exactly = 0) { api.getHomeStats(any()) }
    }

    @Test
    fun `getStats returns expected hardcoded values`() = runTest {
        val data = (repository.getStats() as HomeResult.Success).data
        assertEquals(3, data.successful_today)
        assertEquals(0, data.failed_today)
        assertEquals(518, data.token_balance)
        // Placeholders only: the dashboard replaces airtime_used with the sum
        // of today's successful transactions and airtime_balance with the
        // real SIM reading from the *144# USSD.
        assertEquals(0.0, data.airtime_used, 0.001)
        assertEquals(0.0, data.airtime_balance, 0.001)
        assertEquals(24.0, data.weekly_commission, 0.001)
        assertEquals(7, data.commission_by_day.size)
    }

    // ------------------------------------------------------------------
    // getRecentTransactions()
    // ------------------------------------------------------------------

    @Test
    fun `getRecentTransactions success returns transaction list`() = runTest {
        val transactions = listOf(
            TransactionDto(1, 1, 1, "PAUL ONCHOKE", "0790797274", null, 100.0, "success", "2026-06-17")
        )
        coEvery { api.getTransactions("Bearer test_token") } returns transactions

        val result = repository.getRecentTransactions()

        assertTrue(result is HomeResult.Success)
        assertEquals(transactions, (result as HomeResult.Success).data)
    }

    @Test
    fun `getRecentTransactions network error returns Error with message`() = runTest {
        coEvery { api.getTransactions(any()) } throws Exception("Timeout")

        val result = repository.getRecentTransactions()

        assertTrue(result is HomeResult.Error)
        assertEquals("Timeout", (result as HomeResult.Error).message)
    }

    @Test
    fun `getRecentTransactions with null token returns not authenticated error`() = runTest {
        every { tokenDataStore.accessToken } returns flowOf(null)

        val result = repository.getRecentTransactions()

        assertTrue(result is HomeResult.Error)
        assertEquals("Not authenticated", (result as HomeResult.Error).message)
    }

    @Test
    fun `getRecentTransactions sends correct bearer header`() = runTest {
        val headerSlot = slot<String>()
        coEvery { api.getTransactions(capture(headerSlot)) } returns emptyList()

        repository.getRecentTransactions()

        assertEquals("Bearer test_token", headerSlot.captured)
    }
}
