package com.bytethrux.loadr.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

/**
 * The agent's current entitlement, mirroring the backend UserSubscriptions
 * model: a subscription window ([expiryAt]) with a countable balance of USSD
 * requests ([tokens], i.e. requests_remaining).
 */
data class SubscriptionState(
    val expiryAt: Long = 0L,
    val tokens: Int = 0,
) {
    fun isTimePlanActive(now: Long = System.currentTimeMillis()): Boolean = expiryAt > now

    /** Whether the app is allowed to process payments right now. */
    fun hasCapability(now: Long = System.currentTimeMillis()): Boolean =
        isTimePlanActive(now) && tokens > 0

    /**
     * Tokens usable right now: the remaining count while the subscription is
     * active, zero once it lapses (tokens only exist within a subscription).
     */
    fun availableTokens(now: Long = System.currentTimeMillis()): Int =
        if (isTimePlanActive(now)) tokens else 0

    fun timeRemainingMs(now: Long = System.currentTimeMillis()): Long =
        (expiryAt - now).coerceAtLeast(0L)

    /** "2d 05h 31min" style remaining-time label for the subscriptions screen. */
    fun remainingLabel(now: Long = System.currentTimeMillis()): String {
        val ms = timeRemainingMs(now)
        val days = TimeUnit.MILLISECONDS.toDays(ms)
        val hours = TimeUnit.MILLISECONDS.toHours(ms) % 24
        val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
        return "%dd %02dh %02dmin".format(days, hours, minutes)
    }
}

/**
 * Offline cache of the backend subscription entitlement. Synced by
 * SubscriptionsRepository; consulted by the payment pipeline so gating keeps
 * working without connectivity.
 */
class SubscriptionStore(private val context: Context) {

    companion object {
        private val SUB_EXPIRY_AT = longPreferencesKey("subscription_expiry_at")
        private val TOKENS = intPreferencesKey("subscription_tokens")
        private val LAST_PAYMENT_AT = longPreferencesKey("last_payment_at")

        /**
         * Reconciles the backend token count with the local one. The backend
         * has no decrement endpoint yet, so within the same subscription
         * window (same expiry) the local count — which subtracts one per
         * executed USSD — is the accurate remainder and the smaller of the
         * two wins. A different expiry means a new subscription: adopt the
         * backend count wholesale.
         */
        fun reconcileTokens(
            cachedExpiryAt: Long,
            cachedTokens: Int,
            backendExpiryAt: Long,
            backendTokens: Int,
        ): Int = if (backendExpiryAt == cachedExpiryAt) {
            minOf(cachedTokens, backendTokens)
        } else {
            backendTokens
        }
    }

    val state: Flow<SubscriptionState> = context.dataStore.data.map { prefs ->
        SubscriptionState(
            expiryAt = prefs[SUB_EXPIRY_AT] ?: 0L,
            tokens = prefs[TOKENS] ?: 0,
        )
    }

    suspend fun current(): SubscriptionState = state.first()

    /**
     * Bumped after the background service finishes a payment so open screens
     * (dashboard, transactions) know to refetch tokens, tallies and lists.
     */
    val lastPaymentAt: Flow<Long?> = context.dataStore.data.map { it[LAST_PAYMENT_AT] }

    suspend fun notifyPaymentProcessed() {
        context.dataStore.edit { it[LAST_PAYMENT_AT] = System.currentTimeMillis() }
    }

    /**
     * Mirrors the backend's active subscription into the cache, keeping the
     * locally decremented token count when it is the accurate remainder.
     */
    suspend fun setFromBackend(expiryAt: Long, requestsRemaining: Int) {
        context.dataStore.edit { prefs ->
            val reconciled = reconcileTokens(
                cachedExpiryAt = prefs[SUB_EXPIRY_AT] ?: 0L,
                cachedTokens = prefs[TOKENS] ?: 0,
                backendExpiryAt = expiryAt,
                backendTokens = requestsRemaining,
            )
            prefs[SUB_EXPIRY_AT] = expiryAt
            prefs[TOKENS] = reconciled
        }
    }

    /** No active subscription on the backend. */
    suspend fun clearEntitlement() {
        context.dataStore.edit { prefs ->
            prefs[SUB_EXPIRY_AT] = 0L
            prefs[TOKENS] = 0
        }
    }

    /**
     * Uses up one request locally after a USSD runs. The backend has no
     * decrement endpoint yet; the next sync overwrites this local count.
     */
    suspend fun consumeToken() {
        context.dataStore.edit { prefs ->
            val tokens = prefs[TOKENS] ?: 0
            if (tokens > 0) prefs[TOKENS] = tokens - 1
        }
    }
}
