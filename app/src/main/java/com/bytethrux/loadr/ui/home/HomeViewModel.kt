package com.bytethrux.loadr.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.network.HomeStatsDto
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.HomeResult
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

            // Show the last known SIM balance immediately…
            airtimeBalanceProvider?.cachedBalance()?.let { cached ->
                stats = stats?.copy(airtime_balance = cached)
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    stats = stats,
                    transactions = (txResult as? HomeResult.Success)?.data ?: emptyList(),
                    errorMessage = (statsResult as? HomeResult.Error)?.message
                )
            }

            // …then query the real balance from the SIM via the *144# USSD.
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
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            HomeViewModel(repository, airtimeBalanceProvider) as T
    }
}