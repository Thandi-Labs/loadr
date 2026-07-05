package com.bytethrux.loadr.ui.home

import com.bytethrux.loadr.data.network.HomeStatsDto
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.HomeResult
import com.bytethrux.loadr.data.ussd.AirtimeBalanceProvider
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
        // Tallies are recomputed from today's transactions; the mock
        // transaction is dated 2026-06-17, so it contributes nothing.
        assertEquals(
            mockStats.copy(successful_today = 0, failed_today = 0, airtime_used = 0.0),
            state.stats
        )
        assertEquals(mockTransactions, state.transactions)
        assertNull(state.errorMessage)
    }

    @Test
    fun `airtime used today is the sum of today's successful transactions`() = runTest {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        coEvery { repository.getRecentTransactions() } returns HomeResult.Success(
            listOf(
                TransactionDto(1, 1, 1, "A", "0700000001", null, 20.0, "success", today),
                TransactionDto(2, 1, 2, "B", "0700000002", null, 50.0, "success", today),
                TransactionDto(3, 1, 3, "C", "0700000003", null, 99.0, "failed", today),
                TransactionDto(4, 1, 4, "D", "0700000004", null, 500.0, "success", "2026-01-01"),
            )
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(70.0, viewModel.uiState.value.stats!!.airtime_used, 0.001)
    }

    @Test
    fun `successful and failed cards tally today's transactions`() = runTest {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        coEvery { repository.getRecentTransactions() } returns HomeResult.Success(
            listOf(
                TransactionDto(1, 1, 1, "A", "0700000001", null, 20.0, "success", today),
                TransactionDto(2, 1, 2, "B", "0700000002", null, 50.0, "success", today),
                TransactionDto(3, 1, 3, "C", "0700000003", null, 99.0, "failed", today),
                TransactionDto(4, 1, 4, "D", "0700000004", null, 10.0, "failed", "2026-01-01"),
                TransactionDto(5, 1, 5, "E", "0700000005", null, 30.0, "success", "2026-01-01"),
            )
        )

        viewModel.refresh()
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.stats!!.successful_today)
        assertEquals(1, viewModel.uiState.value.stats!!.failed_today)
    }

    @Test
    fun `payment event from the background service triggers a full refresh`() = runTest {
        val store = mockk<com.bytethrux.loadr.data.local.SubscriptionStore>()
        val paymentSignal =
            kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
        every { store.lastPaymentAt } returns paymentSignal
        coEvery { store.current() } returns com.bytethrux.loadr.data.local.SubscriptionState()

        // Drop calls recorded by the setUp() view model instance.
        clearMocks(repository, answers = false, recordedCalls = true)

        viewModel = HomeViewModel(repository, subscriptionStore = store)
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.getRecentTransactions() }

        // Background service finishes a payment.
        paymentSignal.value = 12345L
        advanceUntilIdle()

        // Stats + transactions (tokens, tallies, airtime used) refetched.
        coVerify(exactly = 2) { repository.getRecentTransactions() }
        coVerify(exactly = 2) { repository.getStats() }
    }

    @Test
    fun `tokens card shows the exact remainder while subscribed`() = runTest {
        val subsRepo = mockk<com.bytethrux.loadr.data.repository.SubscriptionsRepository>()
        coEvery { subsRepo.syncMySubscription() } returns
            com.bytethrux.loadr.data.local.SubscriptionState(
                expiryAt = System.currentTimeMillis() + 86_400_000L,
                tokens = 287,
            )

        viewModel = HomeViewModel(repository, subscriptionsRepository = subsRepo)
        advanceUntilIdle()

        assertEquals(287, viewModel.uiState.value.stats!!.token_balance)
    }

    @Test
    fun `tokens card shows zero when the subscription has lapsed`() = runTest {
        val subsRepo = mockk<com.bytethrux.loadr.data.repository.SubscriptionsRepository>()
        coEvery { subsRepo.syncMySubscription() } returns
            com.bytethrux.loadr.data.local.SubscriptionState(
                expiryAt = System.currentTimeMillis() - 1_000L,
                tokens = 150, // stale local count from the lapsed window
            )

        viewModel = HomeViewModel(repository, subscriptionsRepository = subsRepo)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.stats!!.token_balance)
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

    // ------------------------------------------------------------------
    // Airtime balance (from the SIM via *144#)
    // ------------------------------------------------------------------

    @Test
    fun `airtime balance is replaced by the SIM balance from the provider`() = runTest {
        val provider = mockk<AirtimeBalanceProvider>()
        coEvery { provider.cachedBalance() } returns 120.50
        coEvery { provider.refreshBalance() } returns 130.75

        viewModel = HomeViewModel(repository, provider)
        advanceUntilIdle()

        // The backend/mock figure is discarded in favour of the USSD reading.
        assertEquals(130.75, viewModel.uiState.value.stats!!.airtime_balance, 0.001)
    }

    @Test
    fun `cached SIM balance is shown when the USSD refresh fails`() = runTest {
        val provider = mockk<AirtimeBalanceProvider>()
        coEvery { provider.cachedBalance() } returns 88.25
        coEvery { provider.refreshBalance() } returns null

        viewModel = HomeViewModel(repository, provider)
        advanceUntilIdle()

        assertEquals(88.25, viewModel.uiState.value.stats!!.airtime_balance, 0.001)
    }

    @Test
    fun `airtime balance defaults to zero when no reading exists`() = runTest {
        val provider = mockk<AirtimeBalanceProvider>()
        coEvery { provider.cachedBalance() } returns null
        coEvery { provider.refreshBalance() } returns null

        viewModel = HomeViewModel(repository, provider)
        advanceUntilIdle()

        assertEquals(0.0, viewModel.uiState.value.stats!!.airtime_balance, 0.001)
    }

    @Test
    fun `refreshAirtimeBalance forces a new USSD reading and updates state`() = runTest {
        val provider = mockk<AirtimeBalanceProvider>()
        coEvery { provider.cachedBalance() } returns 10.0
        coEvery { provider.refreshBalance() } returns 10.0
        viewModel = HomeViewModel(repository, provider)
        advanceUntilIdle()

        coEvery { provider.refreshBalance(force = true) } returns 55.5
        viewModel.refreshAirtimeBalance()
        advanceUntilIdle()

        coVerify { provider.refreshBalance(force = true) }
        assertEquals(55.5, viewModel.uiState.value.stats!!.airtime_balance, 0.001)
        assertFalse(viewModel.uiState.value.isAirtimeRefreshing)
    }

    @Test
    fun `refreshAirtimeBalance keeps the current value when the USSD fails`() = runTest {
        val provider = mockk<AirtimeBalanceProvider>()
        coEvery { provider.cachedBalance() } returns 10.0
        coEvery { provider.refreshBalance() } returns 10.0
        viewModel = HomeViewModel(repository, provider)
        advanceUntilIdle()

        coEvery { provider.refreshBalance(force = true) } returns null
        viewModel.refreshAirtimeBalance()
        advanceUntilIdle()

        assertEquals(10.0, viewModel.uiState.value.stats!!.airtime_balance, 0.001)
        assertFalse(viewModel.uiState.value.isAirtimeRefreshing)
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
