package com.bytethrux.loadr.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.bytethrux.loadr.MainActivity
import com.bytethrux.loadr.R
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.CreateTransactionRequest
import com.bytethrux.loadr.data.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume

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

        val success = runUssd(ussdCode)
        val status = if (success) "success" else "failed"

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

        notify(
            if (success) "Loadr - Sent ${offer.offer_name} to $phone"
            else "Loadr - USSD failed for ${offer.offer_name} → $phone"
        )
    }

    private suspend fun runUssd(code: String): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ussdApi26(code)
        } else {
            ussdViaDialIntent(code)
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun ussdApi26(code: String): Boolean {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        return try {
            withTimeoutOrNull(30_000L) {
                suspendCancellableCoroutine { cont ->
                    tm.sendUssdRequest(
                        code,
                        object : TelephonyManager.UssdResponseCallback() {
                            override fun onReceiveUssdResponse(
                                tm: TelephonyManager, request: String, response: CharSequence
                            ) = cont.resume(true)

                            override fun onReceiveUssdResponseFailed(
                                tm: TelephonyManager, request: String, failureCode: Int
                            ) = cont.resume(false)
                        },
                        Handler(Looper.getMainLooper())
                    )
                }
            } ?: false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun ussdViaDialIntent(code: String): Boolean {
        return try {
            val uri = Uri.parse("tel:${code.replace("#", Uri.encode("#"))}")
            startActivity(
                Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
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
