package com.bytethrux.loadr.ui.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.repository.HomeRepository
import com.bytethrux.loadr.data.repository.HomeResult
import com.bytethrux.loadr.data.transactions.DateFilter
import com.bytethrux.loadr.data.transactions.StatusFilter
import com.bytethrux.loadr.data.transactions.TransactionFilters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val allTransactions: List<TransactionDto> = emptyList(),
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val dateFilter: DateFilter = DateFilter.TODAY,
    val searchQuery: String = "",
    val isSearchOpen: Boolean = false,
    val errorMessage: String? = null,
) {
    val filtered: List<TransactionDto>
        get() = TransactionFilters.apply(allTransactions, statusFilter, dateFilter, searchQuery)
}

class TransactionsViewModel(private val repository: HomeRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = repository.getRecentTransactions()) {
                is HomeResult.Success -> _uiState.update {
                    it.copy(isLoading = false, allTransactions = result.data)
                }
                is HomeResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    /** Applies the filter the user arrived with (e.g. from a dashboard card). */
    fun setInitialFilters(status: StatusFilter, date: DateFilter = DateFilter.TODAY) {
        _uiState.update { it.copy(statusFilter = status, dateFilter = date) }
    }

    fun setStatusFilter(filter: StatusFilter) =
        _uiState.update { it.copy(statusFilter = filter) }

    fun setDateFilter(filter: DateFilter) =
        _uiState.update { it.copy(dateFilter = filter) }

    fun setSearchQuery(query: String) =
        _uiState.update { it.copy(searchQuery = query) }

    fun toggleSearch() =
        _uiState.update {
            it.copy(isSearchOpen = !it.isSearchOpen, searchQuery = "")
        }

    class Factory(private val repository: HomeRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            TransactionsViewModel(repository) as T
    }
}
