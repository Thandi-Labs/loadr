package com.bytethrux.loadr.ui.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.local.SettingsDataStore
import com.bytethrux.loadr.data.local.SubscriptionState
import com.bytethrux.loadr.data.network.SubscriptionPlanDto
import com.bytethrux.loadr.data.repository.HomeResult
import com.bytethrux.loadr.data.repository.SubscriptionsRepository
import com.bytethrux.loadr.data.ussd.UssdExecutor
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SubscriptionsUiState(
    val isLoadingPlans: Boolean = true,
    val plans: List<SubscriptionPlanDto> = emptyList(),
    val plansError: String? = null,
    val selectedPlanId: Int? = null,
    val isActivating: Boolean = false,
)

sealed class SubscriptionsEvent {
    data class ShowMessage(val message: String) : SubscriptionsEvent()
}

class SubscriptionsViewModel(
    private val repository: SubscriptionsRepository,
    private val settingsDataStore: SettingsDataStore,
    private val ussdExecutor: UssdExecutor,
) : ViewModel() {

    companion object {
        // Activation payments go to this Bingwa till line: *140*<amount>*<line>#
        const val ACTIVATION_LINE = "0768585724"

        fun activationCode(plan: SubscriptionPlanDto): String =
            "*140*${plan.price.toInt()}*$ACTIVATION_LINE#"
    }

    private val _uiState = MutableStateFlow(SubscriptionsUiState())
    val uiState: StateFlow<SubscriptionsUiState> = _uiState.asStateFlow()

    private val _subscription = MutableStateFlow(SubscriptionState())
    val subscription: StateFlow<SubscriptionState> = _subscription.asStateFlow()

    private val _events = MutableSharedFlow<SubscriptionsEvent>()
    val events: SharedFlow<SubscriptionsEvent> = _events.asSharedFlow()

    init {
        refresh()
    }

    /** Reloads the plan catalogue and the user's entitlement from the backend. */
    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPlans = true, plansError = null) }
            when (val result = repository.getPlans()) {
                is HomeResult.Success -> _uiState.update {
                    it.copy(isLoadingPlans = false, plans = result.data)
                }
                is HomeResult.Error -> _uiState.update {
                    it.copy(isLoadingPlans = false, plansError = result.message)
                }
            }
        }
        viewModelScope.launch {
            _subscription.value = repository.syncMySubscription()
        }
    }

    fun selectPlan(planId: Int) {
        _uiState.update {
            it.copy(selectedPlanId = if (it.selectedPlanId == planId) null else planId)
        }
    }

    fun activateSelectedPlan() {
        val plan = _uiState.value.plans.firstOrNull { it.id == _uiState.value.selectedPlanId }
            ?: return
        if (_uiState.value.isActivating) return

        viewModelScope.launch {
            _uiState.update { it.copy(isActivating = true) }
            try {
                val simSlot = settingsDataStore.settings.first().bingwaSimSlot
                val ussdResult = ussdExecutor.run(activationCode(plan), simSlot)
                if (!ussdResult.success) {
                    _events.emit(
                        SubscriptionsEvent.ShowMessage(
                            "Activation failed${ussdResult.response?.let { r -> " ($r)" } ?: ""}. Please try again."
                        )
                    )
                    return@launch
                }

                // Payment sent — record the purchase on the backend.
                when (val purchase = repository.purchase(plan.id)) {
                    is HomeResult.Success -> {
                        _subscription.value = repository.syncMySubscription()
                        _events.emit(
                            SubscriptionsEvent.ShowMessage("${plan.name} activated successfully")
                        )
                        _uiState.update { it.copy(selectedPlanId = null) }
                    }
                    is HomeResult.Error -> _events.emit(
                        SubscriptionsEvent.ShowMessage(purchase.message)
                    )
                }
            } finally {
                _uiState.update { it.copy(isActivating = false) }
            }
        }
    }

    fun redeemPromoCode(code: String) {
        viewModelScope.launch {
            // Promo codes need backend support; reject politely for now.
            _events.emit(SubscriptionsEvent.ShowMessage("Invalid or expired promo code"))
        }
    }

    class Factory(
        private val repository: SubscriptionsRepository,
        private val settingsDataStore: SettingsDataStore,
        private val ussdExecutor: UssdExecutor,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SubscriptionsViewModel(repository, settingsDataStore, ussdExecutor) as T
    }
}
