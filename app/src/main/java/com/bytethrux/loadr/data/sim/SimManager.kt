package com.bytethrux.loadr.data.sim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

data class SimInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
)

/** Reads the active SIM cards on the device via [SubscriptionManager]. */
object SimManager {

    fun hasPhonePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED

    /** Active SIMs, ordered by slot. Empty if permission is missing. */
    fun activeSims(context: Context): List<SimInfo> {
        if (!hasPhonePermission(context)) return emptyList()
        return try {
            val sm = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                as SubscriptionManager
            @Suppress("MissingPermission")
            (sm.activeSubscriptionInfoList ?: emptyList())
                .map {
                    SimInfo(
                        slotIndex = it.simSlotIndex,
                        subscriptionId = it.subscriptionId,
                        carrierName = it.carrierName?.toString() ?: "SIM ${it.simSlotIndex + 1}",
                    )
                }
                .sortedBy { it.slotIndex }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    /**
     * Subscription id for the given slot index, or null when the slot has no
     * active SIM (callers should fall back to the default TelephonyManager).
     */
    fun subscriptionIdForSlot(context: Context, slotIndex: Int): Int? =
        activeSims(context).firstOrNull { it.slotIndex == slotIndex }?.subscriptionId
}
