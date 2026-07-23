package com.bytethrux.loadr.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bytethrux.loadr.data.local.LoadrSettings
import com.bytethrux.loadr.data.local.ProcessingMode
import com.bytethrux.loadr.data.local.SettingsDataStore
import com.bytethrux.loadr.data.sim.SimInfo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsDataStore: SettingsDataStore,
    val availableSims: List<SimInfo>,
) : ViewModel() {

    val settings: StateFlow<LoadrSettings> = settingsDataStore.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, LoadrSettings())

    fun setDisplayName(value: String) = save { setDisplayName(value) }
    fun setProcessMpesa(value: Boolean) = save { setProcessMpesa(value) }
    fun setProcessTill(value: Boolean) = save { setProcessTill(value) }
    fun setProcessPayBill(value: Boolean) = save { setProcessPayBill(value) }
    fun setProcessSiteLink(value: Boolean) = save { setProcessSiteLink(value) }
    fun setAutoSaveContacts(value: Boolean) = save { setAutoSaveContacts(value) }
    fun setContactNameSuffix(value: String) = save { setContactNameSuffix(value) }
    fun setPaymentSimSlot(value: Int) = save { setPaymentSimSlot(value) }
    fun setBingwaSimSlot(value: Int) = save { setBingwaSimSlot(value) }
    fun setAutoReplySimSlot(value: Int) = save { setAutoReplySimSlot(value) }
    fun setHybridPortal(value: Boolean) = save { setHybridPortal(value) }
    fun setEngageBot(value: Boolean) = save { setEngageBot(value) }
    fun setProcessingMode(value: ProcessingMode) = save { setProcessingMode(value) }
    fun setThemeMode(value: String) = save { setThemeMode(value) }

    fun addAuthorizedSender(sender: String) {
        val trimmed = sender.trim()
        if (trimmed.isEmpty()) return
        val current = settings.value.authorizedSenders
        save { setAuthorizedSenders(current + trimmed) }
    }

    fun removeAuthorizedSender(sender: String) {
        val current = settings.value.authorizedSenders
        save { setAuthorizedSenders(current - sender) }
    }

    fun addToBlacklist(phone: String) {
        val trimmed = phone.trim()
        if (trimmed.isEmpty()) return
        val current = settings.value.blacklist
        save { setBlacklist(current + trimmed) }
    }

    fun removeFromBlacklist(phone: String) {
        val current = settings.value.blacklist
        save { setBlacklist(current - phone) }
    }

    private fun save(block: suspend SettingsDataStore.() -> Unit) {
        viewModelScope.launch { settingsDataStore.block() }
    }

    class Factory(
        private val settingsDataStore: SettingsDataStore,
        private val availableSims: List<SimInfo>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) =
            SettingsViewModel(settingsDataStore, availableSims) as T
    }
}
