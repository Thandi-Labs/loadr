package com.bytethrux.loadr.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.network.HomeStatsDto
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.HomeResult
import com.bytethrux.loadr.data.repository.SubscriptionsRepository
import com.bytethrux.loadr.data.ussd.AirtimeBalanceProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val stats: HomeStatsDto? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository: HomeRepository,
    private val airtimeBalanceProvider: AirtimeBalanceProvider? = null,
    private val subscriptionStore: SubscriptionStore? = null,
    private val subscriptionsRepository: SubscriptionsRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val statsResult = repository.getStats()
            val txResult = repository.getRecentTransactions()
            var stats = (statsResult as? HomeResult.Success)?.data

            // The airtime balance always comes from the SIM (*144#), never
            // from the backend stats. Show the last known value immediately…
            if (airtimeBalanceProvider != null) {
                val cached = airtimeBalanceProvider.cachedBalance()
                stats = stats?.copy(airtime_balance = cached ?: 0.0)
            }
            // Tokens reflect the backend entitlement (requests remaining),
            // synced here and served from the cache when offline.
            if (subscriptionsRepository != null) {
                stats = stats?.copy(token_balance = subscriptionsRepository.syncMySubscription().tokens)
            } else if (subscriptionStore != null) {
                stats = stats?.copy(token_balance = subscriptionStore.current().tokens)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    stats = stats,
                    transactions = (txResult as? HomeResult.Success)?.data ?: emptyList(),
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

    class Factory(
        private val repository: HomeRepository,
        private val airtimeBalanceProvider: AirtimeBalanceProvider? = null,
        private val subscriptionStore: SubscriptionStore? = null,
        private val subscriptionsRepository: SubscriptionsRepository? = null,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            HomeViewModel(
                repository, airtimeBalanceProvider, subscriptionStore, subscriptionsRepository
            ) as T
    }
}