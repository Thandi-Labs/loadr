package com.bytethrux.loadr.data.sim

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat

/**
 * Data representation of a SIM card's information.
 */
data class SimInfo(
    val slotIndex: Int,
    val subscriptionId: Int,
    val carrierName: String,
    val displayName: String,
    val number: String,
    val isEmbedded: Boolean,
    val cardId: Int,
)

/**
 * Manages reading active SIM cards on the device via [SubscriptionManager].
 *
 * Refactored to a class for better dependency injection and following modern Android patterns.
 */
class SimManager(private val context: Context) {

    private val subscriptionManager: SubscriptionManager by lazy {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    }

    /**
     * Checks if the app has the necessary permissions to read SIM information.
     * On API 33+, [Manifest.permission.READ_PHONE_NUMBERS] is preferred for reading the phone number.
     */
    fun hasPhonePermission(): Boolean {
        val hasState = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
                PackageManager.PERMISSION_GRANTED
        
        val hasNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) ==
                    PackageManager.PERMISSION_GRANTED
        } else {
            true // Fallback to READ_PHONE_STATE on older versions
        }
        
        return hasState && hasNumbers
    }

    /** 
     * Returns a list of active SIMs, ordered by slot index.
     * Returns an empty list if permissions are missing or no SIMs are present.
     */
    fun activeSims(): List<SimInfo> {
        // Minimum requirement to list subscriptions is READ_PHONE_STATE
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != 
            PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        return try {
            @Suppress("MissingPermission")
            val infoList = subscriptionManager.activeSubscriptionInfoList ?: emptyList()
            infoList.map { info ->
                SimInfo(
                    slotIndex = info.simSlotIndex,
                    subscriptionId = info.subscriptionId,
                    carrierName = info.carrierName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    displayName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}",
                    number = getPhoneNumber(info.subscriptionId, @Suppress("DEPRECATION") info.number),
                    isEmbedded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.isEmbedded else false,
                    cardId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) info.cardId else -1
                )
            }.sortedBy { it.slotIndex }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /**
     * Retrieves the phone number using the recommended API for the current platform version.
     * [SubscriptionInfo.getNumber] is deprecated since API 33.
     */
    private fun getPhoneNumber(subscriptionId: Int, deprecatedNumber: String?): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                @Suppress("MissingPermission")
                subscriptionManager.getPhoneNumber(subscriptionId)
            } catch (e: Exception) {
                // If getPhoneNumber fails (e.g. permission revoked at runtime), 
                // fallback to the deprecated number if available, though it might be empty.
                deprecatedNumber ?: ""
            }
        } else {
            deprecatedNumber ?: ""
        }
    }

    /**
     * Finds the current slot index for a given subscription ID.
     * Returns -1 if the SIM is no longer active/present.
     */
    fun slotForSubscription(subscriptionId: Int): Int {
        if (subscriptionId == -1) return -1
        return activeSims()
            .firstOrNull { it.subscriptionId == subscriptionId }
            ?.slotIndex ?: -1
    }

    /**
     * Returns the subscription ID for the given slot index, or null when the slot has no active SIM.
     */
    fun subscriptionIdForSlot(slotIndex: Int): Int? =
        activeSims().firstOrNull { it.slotIndex == slotIndex }?.subscriptionId
}
