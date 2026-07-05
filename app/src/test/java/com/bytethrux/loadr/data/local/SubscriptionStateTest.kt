package com.bytethrux.loadr.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class SubscriptionStateTest {

    private val now = 1_800_000_000_000L

    @Test
    fun `no subscription means no capability`() {
        val state = SubscriptionState(expiryAt = 0L, tokens = 0)
        assertFalse(state.hasCapability(now))
    }

    @Test
    fun `active window with requests remaining grants capability`() {
        val state = SubscriptionState(expiryAt = now + 1_000, tokens = 300)
        assertTrue(state.hasCapability(now))
        assertTrue(state.isTimePlanActive(now))
    }

    @Test
    fun `active window with zero requests remaining has no capability`() {
        val state = SubscriptionState(expiryAt = now + 1_000, tokens = 0)
        assertFalse(state.hasCapability(now))
    }

    @Test
    fun `requests remaining after expiry grant no capability`() {
        val state = SubscriptionState(expiryAt = now - 1_000, tokens = 50)
        assertFalse(state.hasCapability(now))
        assertFalse(state.isTimePlanActive(now))
    }

    @Test
    fun `time remaining is clamped to zero after expiry`() {
        val state = SubscriptionState(expiryAt = now - 60_000, tokens = 0)
        assertEquals(0L, state.timeRemainingMs(now))
        assertEquals("0d 00h 00min", state.remainingLabel(now))
    }

    @Test
    fun `remaining label formats days hours minutes`() {
        val remaining = TimeUnit.DAYS.toMillis(2) +
            TimeUnit.HOURS.toMillis(5) +
            TimeUnit.MINUTES.toMillis(31)
        val state = SubscriptionState(expiryAt = now + remaining, tokens = 0)
        assertEquals("2d 05h 31min", state.remainingLabel(now))
    }

    // ------------------------------------------------------------------
    // availableTokens()
    // ------------------------------------------------------------------

    @Test
    fun `available tokens is the exact remainder while subscribed`() {
        val state = SubscriptionState(expiryAt = now + 1_000, tokens = 287)
        assertEquals(287, state.availableTokens(now))
    }

    @Test
    fun `available tokens is zero once the subscription lapses`() {
        val state = SubscriptionState(expiryAt = now - 1_000, tokens = 150)
        assertEquals(0, state.availableTokens(now))
    }

    // ------------------------------------------------------------------
    // SubscriptionStore.reconcileTokens()
    // ------------------------------------------------------------------

    @Test
    fun `same subscription window keeps the locally decremented count`() {
        // Backend still says 300; we've executed 13 USSDs since the sync.
        assertEquals(
            287,
            SubscriptionStore.reconcileTokens(
                cachedExpiryAt = 111L, cachedTokens = 287,
                backendExpiryAt = 111L, backendTokens = 300,
            )
        )
    }

    @Test
    fun `same window trusts the backend when it is lower`() {
        // e.g. another device consumed more, or the backend starts decrementing.
        assertEquals(
            250,
            SubscriptionStore.reconcileTokens(
                cachedExpiryAt = 111L, cachedTokens = 287,
                backendExpiryAt = 111L, backendTokens = 250,
            )
        )
    }

    @Test
    fun `a new subscription window adopts the backend count`() {
        assertEquals(
            600,
            SubscriptionStore.reconcileTokens(
                cachedExpiryAt = 111L, cachedTokens = 2,
                backendExpiryAt = 999L, backendTokens = 600,
            )
        )
    }
}
