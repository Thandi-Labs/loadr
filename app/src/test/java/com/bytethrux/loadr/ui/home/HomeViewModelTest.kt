package com.bytethrux.loadr.ui.home

import com.bytethrux.loadr.data.network.HomeStatsDto
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.HomeResult
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var repository: HomeRepository
    private lateinit var viewModel: HomeViewModel
    private val dispatcher = UnconfinedTestDispatcher()

    private val mockStats = HomeStatsDto(
        successful_today = 3, failed_today = 0, token_balance = 518,
        airtime_used = 65.0, airtime_balance = 284.29, weekly_commission = 24.0,
        commission_by_day = listOf(2.0, 6.0, 6.0, 0.0, 0.0, 2.0, 8.0)
    )
    private val mockTransactions = listOf(
        TransactionDto(1, 1, 1, "PAUL ONCHOKE", "0790797274", null, 100.0, "success", "2026-06-17")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
        coEvery { repository.getStats() } returns HomeResult.Success(mockStats)
        coEvery { repository.getRecentTransactions() } returns HomeResult.Success(mockTransactions)
        // init { refresh() } runs eagerly via UnconfinedTestDispatcher
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ------------------------------------------------------------------
    // refresh()
    // ------------------------------------------------------------------

    @Test
    fun `after construction state has stats and transactions loaded`() {
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mockStats, state.stats)
        assertEquals(mockTransactions, state.transactions)
        assertNull(state.errorMessage)
    }

    @Test
    fun `refresh reloads stats and transactions`() = runTest {
        val newTransactions = listOf(
            TransactionDto(2, 1, 2, "JANE DOE", "0711222333", null, 50.0, "success", "2026-06-27")
        )
        coEvery { repository.getRecentTransactions() } returns HomeResult.Success(newTransactions)

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(newTransactions, viewModel.uiState.value.transactions)
    }

    @Test
    fun `refresh with transactions error shows empty list and no crash`() = runTest {
        coEvery { repository.getRecentTransactions() } returns HomeResult.Error("Network gone")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(emptyList<TransactionDto>(), viewModel.uiState.value.transactions)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `refresh with stats error propagates errorMessage`() = runTest {
        coEvery { repository.getStats() } returns HomeResult.Error("Stats unavailable")

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("Stats unavailable", viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.stats)
    }

    @Test
    fun `refresh sets isLoading true then false`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        coEvery { repository.getStats() } returns HomeResult.Success(mockStats)
        coEvery { repository.getRecentTransactions() } returns HomeResult.Success(mockTransactions)
        viewModel = HomeViewModel(repository)

        viewModel.refresh()
        assertTrue(viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `transaction error does not set errorMessage`() = runTest {
        coEvery { repository.getStats() } returns HomeResult.Success(mockStats)
        coEvery { repository.getRecentTransactions() } returns HomeResult.Error("Timeout")

        viewModel.refresh()
        advanceUntilIdle()

        // errorMessage is only set from stats errors per ViewModel design
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
