package com.bytethrux.loadr.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.network.OfferDto
import com.bytethrux.loadr.data.network.OfferType
import com.bytethrux.loadr.data.repository.OffersRepository
import com.bytethrux.loadr.data.repository.OffersResult
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OffersUiState(
    val isLoading: Boolean = false,
    val offers: List<OfferDto> = emptyList(),
    val errorMessage: String? = null
)

class OffersViewModel(private val repository: OffersRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.getOffers()) {
                is OffersResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, offers = result.data) }
                }
                is OffersResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
            }
        }
    }

    fun createOffer(name: String, description: String, price: Double, type: OfferType, ussd: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val newOffer = OfferDto(
                id = 0,
                offer_name = name,
                ussd = ussd,
                amount = price,
                category = type,
                active = true
            )
            when (val result = repository.createOffer(newOffer)) {
                is OffersResult.Success -> refresh()
                is OffersResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun updateOffer(updatedOffer: OfferDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.updateOffer(updatedOffer)) {
                is OffersResult.Success -> refresh()
                is OffersResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun deleteOffer(offerId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = repository.deleteOffer(offerId)) {
                is OffersResult.Success -> refresh()
                is OffersResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun toggleOfferStatus(offer: OfferDto) {
        updateOffer(offer.copy(active = !offer.active))
    }

    class Factory(private val repository: OffersRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            OffersViewModel(repository) as T
    }
}
