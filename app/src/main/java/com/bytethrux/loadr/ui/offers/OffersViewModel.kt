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
    val errorMessage: String? = null,
    val isActionInProgress: Boolean = false
)

sealed class OffersUiEvent {
    data class ShowSnackbar(val message: String) : OffersUiEvent()
}

class OffersViewModel(private val repository: OffersRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersUiState())
    val uiState: StateFlow<OffersUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<OffersUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

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

    fun createOffer(name: String, price: Double, type: OfferType, ussd: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            val newOffer = OfferDto(
                id = 0,
                offer_name = name,
                ussd = ussd,
                amount = price,
                category = type,
                active = true
            )
            when (val result = repository.createOffer(newOffer)) {
                is OffersResult.Success -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar("Offer created successfully"))
                    refresh()
                }
                is OffersResult.Error -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar(result.message))
                }
            }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun updateOffer(updatedOffer: OfferDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            when (val result = repository.updateOffer(updatedOffer)) {
                is OffersResult.Success -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar("Offer updated successfully"))
                    refresh()
                }
                is OffersResult.Error -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar(result.message))
                }
            }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun deleteOffer(offerId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            when (val result = repository.deleteOffer(offerId)) {
                is OffersResult.Success -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar("Offer deleted successfully"))
                    refresh()
                }
                is OffersResult.Error -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar(result.message))
                }
            }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    fun toggleOfferStatus(offer: OfferDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(isActionInProgress = true) }
            val result = if (offer.active) {
                repository.deactivateOffer(offer.id)
            } else {
                repository.activateOffer(offer.id)
            }
            when (result) {
                is OffersResult.Success -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar(
                        if (offer.active) "Offer deactivated" else "Offer activated"
                    ))
                    refresh()
                }
                is OffersResult.Error -> {
                    _eventFlow.emit(OffersUiEvent.ShowSnackbar(result.message))
                }
            }
            _uiState.update { it.copy(isActionInProgress = false) }
        }
    }

    class Factory(private val repository: OffersRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            OffersViewModel(repository) as T
    }
}
