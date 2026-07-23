package com.bytethrux.loadr.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.bytethrux.loadr.data.local.LoadrSettings
import com.bytethrux.loadr.data.local.SettingsDataStore
import com.bytethrux.loadr.data.sim.SimManager
import com.bytethrux.loadr.data.sms.MpesaParser
import com.bytethrux.loadr.data.sms.PaymentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MpesaSmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress ?: return
        val body = messages.joinToString("") { it.messageBody }
        val receivedOnSlot = extractSlotIndex(context, intent)

        val pendingResult = goAsync()
        scope.launch {
            try {
                handleSms(context, sender, body, receivedOnSlot)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleSms(
        context: Context,
        sender: String,
        body: String,
        receivedOnSlot: Int,
    ) {
        val settings = SettingsDataStore(context).settings.first()

        if (!isMpesaSender(sender) && !isAuthorizedSender(sender, settings)) return

        // Only act on payments arriving on the SIM chosen to receive them
        // (when the slot could not be determined, process anyway).
        if (receivedOnSlot != -1 && receivedOnSlot != settings.paymentSimSlot) return

        val payment = MpesaParser.parse(body) ?: return

        val typeEnabled = when (payment.type) {
            PaymentType.PERSONAL -> settings.processMpesa
            PaymentType.TILL -> settings.processTill
            PaymentType.PAYBILL -> settings.processPayBill
            PaymentType.SITELINK -> settings.processSiteLink
        }
        if (!typeEnabled) return

        if (payment.senderPhone in settings.blacklist) return

        val serviceIntent = Intent(context, LoadrService::class.java).apply {
            action = LoadrService.ACTION_PROCESS_PAYMENT
            putExtra(LoadrService.EXTRA_AMOUNT, payment.amount)
            putExtra(LoadrService.EXTRA_PHONE, payment.senderPhone)
            putExtra(LoadrService.EXTRA_NAME, payment.senderName)
        }
        context.startForegroundService(serviceIntent)
    }

    private fun isMpesaSender(sender: String): Boolean {
        val upper = sender.uppercase()
        return upper == "MPESA" || upper.contains("MPESA") || upper == "SAFARICOM"
    }

    private fun isAuthorizedSender(sender: String, settings: LoadrSettings): Boolean =
        settings.authorizedSenders.any { it.equals(sender, ignoreCase = true) }

    /** Which SIM slot the SMS arrived on; -1 when it can't be determined. */
    private fun extractSlotIndex(context: Context, intent: Intent): Int {
        val extras = intent.extras ?: return -1

        val slot = extras.getInt("android.telephony.extra.SLOT_INDEX", -1)
            .takeIf { it != -1 }
            ?: extras.getInt("slot", -1).takeIf { it != -1 }
            ?: extras.getInt("phone", -1).takeIf { it != -1 }
        if (slot != null) return slot

        // Fall back to mapping the subscription id to its slot.
        val subId = extras.getInt("android.telephony.extra.SUBSCRIPTION_INDEX", -1)
            .takeIf { it != -1 }
            ?: extras.getInt("subscription", -1).takeIf { it != -1 }
            ?: return -1
        return SimManager.activeSims(context)
            .firstOrNull { it.subscriptionId == subId }
            ?.slotIndex ?: -1
    }
}
