package com.bytethrux.loadr.data.ussd

import android.content.Context
import com.bytethrux.loadr.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Supplies the real SIM balances (Airtime and Bonga) for the home screen.
 * The balances are fetched via USSD on the user's Bingwa SIM and cached in
 * DataStore so the dashboard has an instant value between refreshes.
 */
class AirtimeBalanceProvider(
    context: Context,
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context),
    private val ussdExecutor: UssdExecutor = UssdExecutor(context),
) {

    companion object {
        // Don't hammer the carrier: reuse a recent balance within this window.
        private const val MIN_REFRESH_INTERVAL_MS = 60_000L
    }

    /** Last cached balances. */
    suspend fun cachedAirtime(): Double? = settingsDataStore.airtimeBalance.first()
    suspend fun cachedBonga(): Double? = settingsDataStore.bongaBalance.first()

    /**
     * Returns a fresh airtime balance, running USSD when the cache is stale.
     * Skips USSD if SIM configuration needs attention.
     */
    suspend fun refreshAirtime(force: Boolean = false): Double? {
        val settings = settingsDataStore.settings.first()
        val cached = settingsDataStore.airtimeBalance.first()
        val cachedAt = settingsDataStore.airtimeBalanceAt.first() ?: 0L
        
        val fresh = cached != null &&
            System.currentTimeMillis() - cachedAt < MIN_REFRESH_INTERVAL_MS
        if (fresh && !force) return cached

        // Don't run USSD if the SIMs are out of sync or missing.
        if (settings.simsNeedAttention) return cached

        val simSlot = settings.bingwaSimSlot
        val balance = ussdExecutor.fetchAirtimeBalance(simSlot) ?: return cached
        settingsDataStore.saveAirtimeBalance(balance)
        return balance
    }

    /**
     * Returns a fresh Bonga balance, running USSD when the cache is stale.
     * Skips USSD if SIM configuration needs attention.
     */
    suspend fun refreshBonga(force: Boolean = false): Double? {
        val settings = settingsDataStore.settings.first()
        val cached = settingsDataStore.bongaBalance.first()
        val cachedAt = settingsDataStore.bongaBalanceAt.first() ?: 0L
        
        val fresh = cached != null &&
            System.currentTimeMillis() - cachedAt < MIN_REFRESH_INTERVAL_MS
        if (fresh && !force) return cached

        // Don't run USSD if the SIMs are out of sync or missing.
        if (settings.simsNeedAttention) return cached

        val simSlot = settings.bingwaSimSlot
        val balance = ussdExecutor.fetchBongaBalance(simSlot) ?: return cached
        settingsDataStore.saveBongaBalance(balance)
        return balance
    }

    /** Helper for UI to get the cached value without waiting for USSD. */
    suspend fun cachedBalance(): Double? = cachedAirtime()

    /** Legacy method to maintain compatibility with existing callers. */
    suspend fun refreshBalance(force: Boolean = false): Double? = refreshAirtime(force)
}
