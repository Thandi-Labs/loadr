package com.bytethrux.loadr.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telephony.SmsManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.bytethrux.loadr.MainActivity
import com.bytethrux.loadr.R
import com.bytethrux.loadr.data.local.ProcessingMode
import com.bytethrux.loadr.data.local.SettingsDataStore
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.CreateTransactionRequest
import com.bytethrux.loadr.data.network.RetrofitClient
import com.bytethrux.loadr.data.sim.SimManager
import com.bytethrux.loadr.data.ussd.UssdExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoadrService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val ACTION_PROCESS_PAYMENT = "com.bytethrux.loadr.ACTION_PROCESS_PAYMENT"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_NAME = "extra_name"

        private const val CHANNEL_ID = "loadr_channel"
        private const val NOTIFICATION_ID = 42
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(TokenDataStore(applicationContext))
        createChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Loadr - Ready"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PROCESS_PAYMENT) {
            val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
            val phone = intent.getStringExtra(EXTRA_PHONE) ?: return START_NOT_STICKY
            val name = intent.getStringExtra(EXTRA_NAME) ?: "Customer"

            scope.launch {
                processPayment(amount, phone, name)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun processPayment(amount: Double, phone: String, customerName: String) {
        val settings = SettingsDataStore(applicationContext).settings.first()

        if (settings.autoSaveContacts) {
            ContactSaver.saveIfNew(
                applicationContext, customerName, phone, settings.contactNameSuffix
            )
        }

        val token = TokenDataStore(applicationContext).accessToken.first()
        if (token == null) {
            notify("Loadr - Not signed in, skipped payment from $phone")
            return
        }
        val bearer = "Bearer $token"
        val api = RetrofitClient.instance

        val offers = try {
            api.getOffers(bearer)
        } catch (e: Exception) {
            notify("Loadr - Could not load offers (${e.message})")
            return
        }

        // Match by amount; ignore cents rounding with a 0.01 tolerance
        val offer = offers.firstOrNull { it.active && kotlin.math.abs(it.amount - amount) < 0.01 }
        if (offer == null) {
            notify("Loadr - No active offer found for Ksh${amount.toLong()} from $phone")
            return
        }

        // Replace "LD" placeholder with the customer's phone number
        val ussdCode = offer.ussd.replace("LD", phone)
        notify("Loadr - Sending ${offer.offer_name} to $phone…")

        val result = UssdExecutor(applicationContext).run(
            code = ussdCode,
            simSlot = settings.bingwaSimSlot,
            multiStep = settings.processingMode == ProcessingMode.ADVANCED,
        )
        val status = if (result.success) "success" else "failed"

        try {
            api.createTransaction(
                bearer,
                CreateTransactionRequest(
                    offer_id = offer.id,
                    customer_name = customerName,
                    customer_phone = phone,
                    amount = amount.toInt(),
                    status = status,
                    created_at = todayDate(),
                )
            )
        } catch (_: Exception) {
            // Log failure silently; USSD may have already executed
        }

        if (result.success && settings.engageBot) {
            sendAutoReply(
                phone = phone,
                name = customerName,
                offerName = offer.offer_name,
                simSlot = settings.autoReplySimSlot,
            )
        }

        notify(
            if (result.success) "Loadr - Sent ${offer.offer_name} to $phone"
            else "Loadr - USSD failed for ${offer.offer_name} → $phone"
        )
    }

    private fun sendAutoReply(phone: String, name: String, offerName: String, simSlot: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val message =
            "Hi ${name.split(" ").first().lowercase().replaceFirstChar { it.uppercase() }}, " +
                "your $offerName has been processed. Thank you for your purchase!"
        try {
            val subId = SimManager.subscriptionIdForSlot(applicationContext, simSlot)
            val smsManager = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                    val base = getSystemService(SmsManager::class.java)
                    if (subId != null) base.createForSubscriptionId(subId) else base
                }
                subId != null -> @Suppress("DEPRECATION")
                    SmsManager.getSmsManagerForSubscriptionId(subId)
                else -> @Suppress("DEPRECATION") SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phone, null, message, null, null)
        } catch (_: Exception) {
            // Auto-replies are best-effort.
        }
    }

    private fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun notify(message: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, buildNotification(message))
    }

    private fun buildNotification(message: String): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Loadr")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(tap)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Loadr Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Background M-Pesa offer processing" }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
