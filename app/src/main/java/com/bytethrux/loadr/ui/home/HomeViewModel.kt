package com.bytethrux.loadr.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.network.HomeStatsDto
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.HomeResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val stats: HomeStatsDto? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val errorMessage: String? = null
)

class HomeViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val statsResult = repository.getStats()
            val txResult = repository.getRecentTransactions()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    stats = (statsResult as? HomeResult.Success)?.data,
                    transactions = (txResult as? HomeResult.Success)?.data ?: emptyList(),
                    errorMessage = (statsResult as? HomeResult.Error)?.message
                )
            }
        }
    }

    class Factory(private val repository: HomeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            HomeViewModel(repository) as T
    }
}