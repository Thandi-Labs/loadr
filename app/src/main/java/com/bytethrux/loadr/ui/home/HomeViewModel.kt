package com.bytethrux.loadr.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.network.HomeStatsDto
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.local.SettingsDataStore
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.HomeResult
import com.bytethrux.loadr.data.repository.SubscriptionsRepository
import com.bytethrux.loadr.data.transactions.StatusFilter
import com.bytethrux.loadr.data.transactions.TransactionFilters
import com.bytethrux.loadr.data.ussd.AirtimeBalanceProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val stats: HomeStatsDto? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val errorMessage: String? = null,
    val isAirtimeRefreshing: Boolean = false,
    val hideAirtime: Boolean = false,
)

class HomeViewModel(
    private val repository: HomeRepository,
    private val airtimeBalanceProvider: AirtimeBalanceProvider? = null,
    private val subscriptionStore: SubscriptionStore? = null,
    private val subscriptionsRepository: SubscriptionsRepository? = null,
    private val settingsDataStore: SettingsDataStore? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
        if (settingsDataStore != null) {
            viewModelScope.launch {
                settingsDataStore.settings.collect { settings ->
                    _uiState.update { it.copy(hideAirtime = settings.hideAirtime) }
                }
            }
        }
    }

    /** Masks/unmasks the airtime figures on the dashboard; persisted. */
    fun toggleHideAirtime() {
        val newValue = !_uiState.value.hideAirtime
        _uiState.update { it.copy(hideAirtime = newValue) }
        if (settingsDataStore != null) {
            viewModelScope.launch { settingsDataStore.setHideAirtime(newValue) }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val statsResult = repository.getStats()
            val txResult = repository.getRecentTransactions()
            val transactions = (txResult as? HomeResult.Success)?.data ?: emptyList()
            var stats = (statsResult as? HomeResult.Success)?.data

            // Today's tallies come from the actual transactions: counts for
            // the Successful/Failed cards and the airtime spent.
            stats = stats?.copy(
                successful_today = TransactionFilters.countToday(transactions, StatusFilter.SUCCESSFUL),
                failed_today = TransactionFilters.countToday(transactions, StatusFilter.FAILED),
                airtime_used = TransactionFilters.airtimeUsedToday(transactions),
            )

            // The airtime balance always comes from the SIM (*144#), never
            // from the backend stats. Show the last known value immediately…
            if (airtimeBalanceProvider != null) {
                val cached = airtimeBalanceProvider.cachedBalance()
                stats = stats?.copy(airtime_balance = cached ?: 0.0)
            }
            // Tokens: the exact remainder while subscribed (each USSD
            // subtracts one), zero when the subscription has lapsed.
            if (subscriptionsRepository != null) {
                stats = stats?.copy(
                    token_balance = subscriptionsRepository.syncMySubscription().availableTokens()
                )
            } else if (subscriptionStore != null) {
                stats = stats?.copy(token_balance = subscriptionStore.current().availableTokens())
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    stats = stats,
                    transactions = transactions,
                    errorMessage = (statsResult as? HomeResult.Error)?.message
                )
            }

            // …then run the *144# USSD for a fresh reading.
            if (airtimeBalanceProvider != null && stats != null) {
                val balance = airtimeBalanceProvider.refreshBalance()
                if (balance != null) {
                    _uiState.update { state ->
                        state.copy(stats = state.stats?.copy(airtime_balance = balance))
                    }
                }
            }
        }
    }

    /** Re-runs the *144# USSD immediately (refresh button on the airtime card). */
    fun refreshAirtimeBalance() {
        if (airtimeBalanceProvider == null || _uiState.value.isAirtimeRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isAirtimeRefreshing = true) }
            try {
                val balance = airtimeBalanceProvider.refreshBalance(force = true)
                if (balance != null) {
                    _uiState.update { state ->
                        state.copy(stats = state.stats?.copy(airtime_balance = balance))
                    }
                }
            } finally {
                _uiState.update { it.copy(isAirtimeRefreshing = false) }
            }
        }
    }

    class Factory(
        private val repository: HomeRepository,
        private val airtimeBalanceProvider: AirtimeBalanceProvider? = null,
        private val subscriptionStore: SubscriptionStore? = null,
        private val subscriptionsRepository: SubscriptionsRepository? = null,
        private val settingsDataStore: SettingsDataStore? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            HomeViewModel(
                repository,
                airtimeBalanceProvider,
                subscriptionStore,
                subscriptionsRepository,
                settingsDataStore,
            ) as T
    }
}