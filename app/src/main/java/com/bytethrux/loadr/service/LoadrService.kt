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
import com.bytethrux.loadr.data.local.SettingsDataStore
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.RetrofitClient
import com.bytethrux.loadr.data.repository.SubscriptionsRepository
import com.bytethrux.loadr.data.sim.SimManager
import com.bytethrux.loadr.data.ussd.UssdExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class LoadrService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val simManager by lazy { SimManager(applicationContext) }

    // Payments are accepted concurrently and queued; a single worker executes
    // them in arrival order because the modem runs one USSD session at a time.
    private data class PaymentJob(val amount: Double, val phone: String, val name: String)

    private val paymentQueue = Channel<PaymentJob>(Channel.UNLIMITED)
    private val pendingJobs = AtomicInteger(0)

    @Volatile
    private var lastStartId = -1

    companion object {
        const val ACTION_PROCESS_PAYMENT = "com.bytethrux.loadr.ACTION_PROCESS_PAYMENT"
        const val EXTRA_AMOUNT = "extra_amount"
        const val EXTRA_PHONE = "extra_phone"
        const val EXTRA_NAME = "extra_name"

        private const val CHANNEL_ID = "loadr_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 42
        private const val FIRST_STATUS_NOTIFICATION_ID = 100

        // Process-wide so payment status notifications stay unique even when
        // the service is torn down and recreated between bursts of SMS.
        private val nextStatusNotificationId = AtomicInteger(FIRST_STATUS_NOTIFICATION_ID)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        RetrofitClient.initialize(TokenDataStore(applicationContext))
        createChannel()
        startForeground(FOREGROUND_NOTIFICATION_ID, buildNotification("Loadr - Ready"))

        scope.launch {
            for (job in paymentQueue) {
                try {
                    processPayment(job.amount, job.phone, job.name)
                } finally {
                    val remaining = pendingJobs.decrementAndGet()
                    if (remaining == 0) {
                        // stopSelf(startId) is a no-op if a newer start
                        // arrived in the meantime, so queued work is safe.
                        stopSelf(lastStartId)
                    } else {
                        updateForegroundStatus()
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        if (intent?.action == ACTION_PROCESS_PAYMENT) {
            val amount = intent.getDoubleExtra(EXTRA_AMOUNT, 0.0)
            val phone = intent.getStringExtra(EXTRA_PHONE)
            if (phone != null) {
                val name = intent.getStringExtra(EXTRA_NAME) ?: "Customer"
                pendingJobs.incrementAndGet()
                updateForegroundStatus()
                paymentQueue.trySend(PaymentJob(amount, phone, name))
            } else {
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    private fun updateForegroundStatus() {
        val pending = pendingJobs.get()
        val text = when {
            pending <= 0 -> "Loadr - Ready"
            pending == 1 -> "Loadr - Processing 1 payment…"
            else -> "Loadr - Processing $pending payments…"
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(FOREGROUND_NOTIFICATION_ID, buildNotification(text))
    }

    private suspend fun processPayment(amount: Double, phone: String, customerName: String) {
        val statusId = nextStatusNotificationId.getAndIncrement()
        val settings = SettingsDataStore(applicationContext).settings.first()

        if (settings.autoSaveContacts) {
            ContactSaver.saveIfNew(
                applicationContext, customerName, phone, settings.contactNameSuffix
            )
        }

        val tokenDataStore = TokenDataStore(applicationContext)
        val subscriptionStore = SubscriptionStore(applicationContext)
        val processor = PaymentProcessor(
            api = RetrofitClient.instance,
            tokenDataStore = tokenDataStore,
            subscriptionStore = subscriptionStore,
            subscriptionsRepository = SubscriptionsRepository(
                RetrofitClient.instance, tokenDataStore, subscriptionStore
            ),
            ussdExecutor = UssdExecutor(applicationContext),
        )

        val outcome = processor.process(
            amount = amount,
            phone = phone,
            customerName = customerName,
            settings = settings,
            onSending = { offerName -> notify(statusId, "Loadr - Sending $offerName to $phone…") },
        )

        when (outcome) {
            is PaymentProcessor.Outcome.NoEntitlement -> notify(
                statusId,
                "Loadr - No active subscription or tokens. Ksh${amount.toLong()} from $phone was not processed."
            )
            is PaymentProcessor.Outcome.NotSignedIn ->
                notify(statusId, "Loadr - Not signed in, skipped payment from $phone")
            is PaymentProcessor.Outcome.OffersUnavailable ->
                notify(statusId, "Loadr - Could not load offers (${outcome.reason})")
            is PaymentProcessor.Outcome.NoMatchingOffer ->
                notify(statusId, "Loadr - No active offer found for Ksh${amount.toLong()} from $phone")
            is PaymentProcessor.Outcome.Sent -> {
                if (settings.engageBot) {
                    sendAutoReply(
                        phone = phone,
                        name = customerName,
                        offerName = outcome.offer.offer_name,
                        simSlot = settings.autoReplySimSlot,
                    )
                }
                notify(statusId, "Loadr - Sent ${outcome.offer.offer_name} to $phone")
            }
            is PaymentProcessor.Outcome.UssdFailed ->
                notify(statusId, "Loadr - USSD failed for ${outcome.offer.offer_name} → $phone")
        }

        // A transaction was recorded (and on success a token consumed) —
        // signal open screens to refetch tokens, tallies and transactions.
        if (outcome is PaymentProcessor.Outcome.Sent ||
            outcome is PaymentProcessor.Outcome.UssdFailed
        ) {
            subscriptionStore.notifyPaymentProcessed()
        }
    }

    private fun sendAutoReply(phone: String, name: String, offerName: String, simSlot: Int) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val message =
            "Hi ${name.split(" ").first().lowercase().replaceFirstChar { it.uppercase() }}, " +
                "your $offerName has been processed. Thank you for your purchase!"
        try {
            val subId = simManager.subscriptionIdForSlot(simSlot)
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

    private fun notify(id: Int, message: String) {
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .notify(id, buildNotification(message))
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
