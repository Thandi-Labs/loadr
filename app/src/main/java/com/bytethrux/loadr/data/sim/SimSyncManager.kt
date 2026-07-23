package com.bytethrux.loadr.data.sim

import android.content.Context
import android.util.Log
import com.bytethrux.loadr.data.local.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Ensures that if a user swaps their SIM cards physically, Loadr's settings
 * (which slot to use for USSD/SMS) are updated to follow the unique SIM
 * identity (subscriptionId) rather than staying stuck on a physical slot.
 */
class SimSyncManager(
    private val context: Context,
    private val simManager: SimManager = SimManager(context),
    private val settingsDataStore: SettingsDataStore = SettingsDataStore(context)
) {
    companion object {
        private const val TAG = "SimSyncManager"
    }

    /**
     * Compares the current active SIMs with the saved subscription IDs.
     * If any SIM has moved or is missing, flags for attention.
     * If [LoadrSettings.autoRemapSims] is true, automatically updates slots.
     */
    suspend fun syncSlotsWithSubscriptions() {
        val settings = settingsDataStore.settings.first()
        val activeSims = simManager.activeSims()
        
        if (activeSims.isEmpty()) {
            if (settings.paymentSubId != -1 || settings.bingwaSubId != -1) {
                settingsDataStore.setSimsNeedAttention(true)
            }
            return
        }

        Log.d(TAG, "Syncing SIM slots. Active subscriptions: ${activeSims.map { it.subscriptionId }}")

        var attentionRequired = false

        // 1. Check/Sync Payment SIM
        if (settings.paymentSubId != -1) {
            val info = activeSims.firstOrNull { it.subscriptionId == settings.paymentSubId }
            if (info != null) {
                if (info.slotIndex != settings.paymentSimSlot) {
                    if (settings.autoRemapSims) {
                        Log.i(TAG, "Auto-remapping Payment SIM to slot ${info.slotIndex}")
                        settingsDataStore.setPaymentSimSlot(info.slotIndex, settings.paymentSubId)
                    } else {
                        Log.i(TAG, "Payment SIM moved from slot ${settings.paymentSimSlot} to ${info.slotIndex}")
                        attentionRequired = true
                    }
                }
            } else {
                Log.w(TAG, "Payment SIM (ID: ${settings.paymentSubId}) is missing!")
                attentionRequired = true
            }
        }

        // 2. Check/Sync Bingwa SIM (USSD)
        if (settings.bingwaSubId != -1) {
            val info = activeSims.firstOrNull { it.subscriptionId == settings.bingwaSubId }
            if (info != null) {
                if (info.slotIndex != settings.bingwaSimSlot) {
                    if (settings.autoRemapSims) {
                        Log.i(TAG, "Auto-remapping Bingwa SIM to slot ${info.slotIndex}")
                        settingsDataStore.setBingwaSimSlot(info.slotIndex, settings.bingwaSubId)
                    } else {
                        Log.i(TAG, "Bingwa SIM moved from slot ${settings.bingwaSimSlot} to ${info.slotIndex}")
                        attentionRequired = true
                    }
                }
            } else {
                Log.w(TAG, "Bingwa SIM (ID: ${settings.bingwaSubId}) is missing!")
                attentionRequired = true
            }
        }

        // 3. Check/Sync Auto-Reply SIM
        if (settings.autoReplySubId != -1) {
            val info = activeSims.firstOrNull { it.subscriptionId == settings.autoReplySubId }
            if (info != null) {
                if (info.slotIndex != settings.autoReplySimSlot) {
                    if (settings.autoRemapSims) {
                        Log.i(TAG, "Auto-remapping Auto-reply SIM to slot ${info.slotIndex}")
                        settingsDataStore.setAutoReplySimSlot(info.slotIndex, settings.autoReplySubId)
                    } else {
                        Log.i(TAG, "Auto-reply SIM moved from slot ${settings.autoReplySimSlot} to ${info.slotIndex}")
                        attentionRequired = true
                    }
                }
            } else {
                Log.w(TAG, "Auto-reply SIM (ID: ${settings.autoReplySubId}) is missing.")
                attentionRequired = true
            }
        }

        if (settings.simsNeedAttention != attentionRequired) {
            settingsDataStore.setSimsNeedAttention(attentionRequired)
        }
    }
}
