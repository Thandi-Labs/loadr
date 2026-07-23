package com.bytethrux.loadr.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ProcessingMode { EXPRESS, ADVANCED;

    companion object {
        fun fromString(value: String?): ProcessingMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPRESS
    }
}

/** Snapshot of every user-configurable setting. */
data class LoadrSettings(
    val displayName: String = "",
    // Message processing
    val processMpesa: Boolean = true,
    val processTill: Boolean = true,
    val processPayBill: Boolean = false,
    val processSiteLink: Boolean = true,
    val authorizedSenders: Set<String> = emptySet(),
    // My customers
    val autoSaveContacts: Boolean = false,
    val contactNameSuffix: String = "Loadr",
    val blacklist: Set<String> = emptySet(),
    // SIM setup (slot indices: 0 = SIM 1, 1 = SIM 2)
    val paymentSimSlot: Int = 0,
    val bingwaSimSlot: Int = 0,
    val autoReplySimSlot: Int = 0,
    // Hybrid portal & customer tools
    val hybridPortal: Boolean = false,
    val engageBot: Boolean = true,
    // Processing mode
    val processingMode: ProcessingMode = ProcessingMode.EXPRESS,
    // Appearance: "system" | "light" | "dark"
    val themeMode: String = "dark",
    // Mask the airtime figures on the dashboard
    val hideAirtime: Boolean = false,
)

class SettingsDataStore(private val context: Context) {

    companion object {
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
        private val PROCESS_MPESA = booleanPreferencesKey("process_mpesa")
        private val PROCESS_TILL = booleanPreferencesKey("process_till")
        private val PROCESS_PAYBILL = booleanPreferencesKey("process_paybill")
        private val PROCESS_SITELINK = booleanPreferencesKey("process_sitelink")
        private val AUTHORIZED_SENDERS = stringSetPreferencesKey("authorized_senders")
        private val AUTO_SAVE_CONTACTS = booleanPreferencesKey("auto_save_contacts")
        private val CONTACT_NAME_SUFFIX = stringPreferencesKey("contact_name_suffix")
        private val BLACKLIST = stringSetPreferencesKey("blacklist")
        private val PAYMENT_SIM_SLOT = intPreferencesKey("payment_sim_slot")
        private val BINGWA_SIM_SLOT = intPreferencesKey("bingwa_sim_slot")
        private val AUTOREPLY_SIM_SLOT = intPreferencesKey("autoreply_sim_slot")
        private val HYBRID_PORTAL = booleanPreferencesKey("hybrid_portal")
        private val ENGAGE_BOT = booleanPreferencesKey("engage_bot")
        private val PROCESSING_MODE = stringPreferencesKey("processing_mode")
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val HIDE_AIRTIME = booleanPreferencesKey("hide_airtime")

        // Airtime balance cache (fetched via *144# USSD)
        private val AIRTIME_BALANCE = doublePreferencesKey("airtime_balance")
        private val AIRTIME_BALANCE_AT = longPreferencesKey("airtime_balance_at")
    }

    val settings: Flow<LoadrSettings> = context.dataStore.data.map { prefs ->
        LoadrSettings(
            displayName = prefs[DISPLAY_NAME] ?: "",
            processMpesa = prefs[PROCESS_MPESA] ?: true,
            processTill = prefs[PROCESS_TILL] ?: true,
            processPayBill = prefs[PROCESS_PAYBILL] ?: false,
            processSiteLink = prefs[PROCESS_SITELINK] ?: true,
            authorizedSenders = prefs[AUTHORIZED_SENDERS] ?: emptySet(),
            autoSaveContacts = prefs[AUTO_SAVE_CONTACTS] ?: false,
            contactNameSuffix = prefs[CONTACT_NAME_SUFFIX] ?: "Loadr",
            blacklist = prefs[BLACKLIST] ?: emptySet(),
            paymentSimSlot = prefs[PAYMENT_SIM_SLOT] ?: 0,
            bingwaSimSlot = prefs[BINGWA_SIM_SLOT] ?: 0,
            autoReplySimSlot = prefs[AUTOREPLY_SIM_SLOT] ?: 0,
            hybridPortal = prefs[HYBRID_PORTAL] ?: false,
            engageBot = prefs[ENGAGE_BOT] ?: true,
            processingMode = ProcessingMode.fromString(prefs[PROCESSING_MODE]),
            themeMode = prefs[THEME_MODE] ?: "dark",
            hideAirtime = prefs[HIDE_AIRTIME] ?: false,
        )
    }

    val airtimeBalance: Flow<Double?> = context.dataStore.data.map { it[AIRTIME_BALANCE] }
    val airtimeBalanceAt: Flow<Long?> = context.dataStore.data.map { it[AIRTIME_BALANCE_AT] }

    suspend fun setDisplayName(value: String) = edit { it[DISPLAY_NAME] = value }
    suspend fun setProcessMpesa(value: Boolean) = edit { it[PROCESS_MPESA] = value }
    suspend fun setProcessTill(value: Boolean) = edit { it[PROCESS_TILL] = value }
    suspend fun setProcessPayBill(value: Boolean) = edit { it[PROCESS_PAYBILL] = value }
    suspend fun setProcessSiteLink(value: Boolean) = edit { it[PROCESS_SITELINK] = value }
    suspend fun setAuthorizedSenders(value: Set<String>) = edit { it[AUTHORIZED_SENDERS] = value }
    suspend fun setAutoSaveContacts(value: Boolean) = edit { it[AUTO_SAVE_CONTACTS] = value }
    suspend fun setContactNameSuffix(value: String) = edit { it[CONTACT_NAME_SUFFIX] = value }
    suspend fun setBlacklist(value: Set<String>) = edit { it[BLACKLIST] = value }
    suspend fun setPaymentSimSlot(value: Int) = edit { it[PAYMENT_SIM_SLOT] = value }
    suspend fun setBingwaSimSlot(value: Int) = edit { it[BINGWA_SIM_SLOT] = value }
    suspend fun setAutoReplySimSlot(value: Int) = edit { it[AUTOREPLY_SIM_SLOT] = value }
    suspend fun setHybridPortal(value: Boolean) = edit { it[HYBRID_PORTAL] = value }
    suspend fun setEngageBot(value: Boolean) = edit { it[ENGAGE_BOT] = value }
    suspend fun setProcessingMode(value: ProcessingMode) = edit { it[PROCESSING_MODE] = value.name.lowercase() }
    suspend fun setThemeMode(value: String) = edit { it[THEME_MODE] = value }
    suspend fun setHideAirtime(value: Boolean) = edit { it[HIDE_AIRTIME] = value }

    suspend fun saveAirtimeBalance(balance: Double) = edit {
        it[AIRTIME_BALANCE] = balance
        it[AIRTIME_BALANCE_AT] = System.currentTimeMillis()
    }

    private suspend fun edit(
        block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit
    ) {
        context.dataStore.edit(block)
    }
}
