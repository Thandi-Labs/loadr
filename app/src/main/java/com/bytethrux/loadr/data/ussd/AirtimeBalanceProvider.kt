package com.bytethrux.loadr.data.ussd

import android.content.Context
import com.bytethrux.loadr.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Supplies the real SIM airtime balance for the home screen. The balance is
 * fetched via the *144# USSD on the user's Bingwa SIM and cached in
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

    /** Last cached balance, or null if it was never fetched. */
    suspend fun cachedBalance(): Double? = settingsDataStore.airtimeBalance.first()

    /**
     * Returns a fresh balance, running the *144# USSD when the cache is stale.
     * Falls back to the cached value when the USSD fails (no SIM, permission
     * denied, timeout).
     */
    suspend fun refreshBalance(force: Boolean = false): Double? {
        val cached = settingsDataStore.airtimeBalance.first()
        val cachedAt = settingsDataStore.airtimeBalanceAt.first() ?: 0L
        val fresh = cached != null &&
            System.currentTimeMillis() - cachedAt < MIN_REFRESH_INTERVAL_MS
        if (fresh && !force) return cached

        val simSlot = settingsDataStore.settings.first().bingwaSimSlot
        val balance = ussdExecutor.fetchAirtimeBalance(simSlot) ?: return cached
        settingsDataStore.saveAirtimeBalance(balance)
        return balance
    }
}
