package com.bytethrux.loadr.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.bytethrux.loadr.data.sms.MpesaParser

class MpesaSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val sender = messages.firstOrNull()?.originatingAddress ?: return

        if (!isMpesaSender(sender)) return

        val body = messages.joinToString("") { it.messageBody }
        val payment = MpesaParser.parse(body) ?: return

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
}
