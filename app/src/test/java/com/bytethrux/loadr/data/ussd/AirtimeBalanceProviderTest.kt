package com.bytethrux.loadr.data.ussd

import android.content.Context
import com.bytethrux.loadr.data.local.LoadrSettings
import com.bytethrux.loadr.data.local.SettingsDataStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Mock-driven checks that the *144# balance flow reads from the right SIM,
 * caches correctly, and never loses a good reading to a failed USSD.
 */
class AirtimeBalanceProviderTest {

    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var ussdExecutor: UssdExecutor
    private lateinit var provider: AirtimeBalanceProvider

    @Before
    fun setUp() {
        settingsDataStore = mockk(relaxed = true)
        ussdExecutor = mockk()
        provider = AirtimeBalanceProvider(
            context = mockk<Context>(relaxed = true),
            settingsDataStore = settingsDataStore,
            ussdExecutor = ussdExecutor,
        )
        every { settingsDataStore.settings } returns flowOf(LoadrSettings(bingwaSimSlot = 1))
    }

    private fun cache(balance: Double?, ageMs: Long = 0L) {
        every { settingsDataStore.airtimeBalance } returns flowOf(balance)
        every { settingsDataStore.airtimeBalanceAt } returns flowOf(
            balance?.let { System.currentTimeMillis() - ageMs }
        )
    }

    @Test
    fun `fresh cache is served without dialing the USSD`() = runTest {
        cache(284.29, ageMs = 10_000)

        assertEquals(284.29, provider.refreshBalance()!!, 0.001)
        coVerify(exactly = 0) { ussdExecutor.fetchAirtimeBalance(any()) }
    }

    @Test
    fun `stale cache triggers a new star-144 read on the Bingwa SIM`() = runTest {
        cache(100.0, ageMs = 120_000)
        coEvery { ussdExecutor.fetchAirtimeBalance(1) } returns 250.75

        assertEquals(250.75, provider.refreshBalance()!!, 0.001)
        coVerify { ussdExecutor.fetchAirtimeBalance(simSlot = 1) }
        coVerify { settingsDataStore.saveAirtimeBalance(250.75) }
    }

    @Test
    fun `force bypasses a fresh cache`() = runTest {
        cache(100.0, ageMs = 5_000)
        coEvery { ussdExecutor.fetchAirtimeBalance(1) } returns 90.5

        assertEquals(90.5, provider.refreshBalance(force = true)!!, 0.001)
        coVerify { ussdExecutor.fetchAirtimeBalance(any()) }
    }

    @Test
    fun `failed USSD keeps the cached balance and stores nothing`() = runTest {
        cache(100.0, ageMs = 120_000)
        coEvery { ussdExecutor.fetchAirtimeBalance(any()) } returns null

        assertEquals(100.0, provider.refreshBalance()!!, 0.001)
        coVerify(exactly = 0) { settingsDataStore.saveAirtimeBalance(any()) }
    }

    @Test
    fun `no cache and failed USSD returns null`() = runTest {
        cache(null)
        coEvery { ussdExecutor.fetchAirtimeBalance(any()) } returns null

        assertNull(provider.refreshBalance())
    }

    @Test
    fun `cachedBalance exposes the stored reading`() = runTest {
        cache(42.42)
        assertEquals(42.42, provider.cachedBalance()!!, 0.001)
    }
}
