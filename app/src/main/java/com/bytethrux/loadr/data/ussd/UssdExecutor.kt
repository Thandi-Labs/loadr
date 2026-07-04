package com.bytethrux.loadr.data.ussd

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.bytethrux.loadr.data.sim.SimManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class UssdResult(
    val success: Boolean,
    val response: String? = null,
)

/**
 * Runs USSD codes on a specific SIM slot. On API 26+ this uses
 * [TelephonyManager.sendUssdRequest] against the subscription bound to the
 * chosen slot; older devices fall back to a dial intent.
 *
 * Multi-step ("advanced") codes are written as steps separated by ";" or
 * newlines — each step is dispatched as its own USSD request, in order.
 */
class UssdExecutor(private val context: Context) {

    companion object {
        const val BALANCE_CODE = "*144#"
        private const val USSD_TIMEOUT_MS = 35_000L
        private const val STEP_DELAY_MS = 2_000L

        // The modem handles one USSD session at a time. All executors share
        // this lock so concurrent payment/balance requests queue up instead
        // of colliding mid-session.
        private val ussdLock = Mutex()

        private val STEP_SEPARATOR = Regex("""[;\n]+""")

        // "Your account balance is Ksh 284.29" / "balance: Kshs. 1,024.50"
        private val BALANCE_REGEX = Regex("""[Kk][Ss][Hh][Ss]?\.?\s*([\d,]+(?:\.\d{1,2})?)""")

        fun parseBalance(response: String?): Double? {
            if (response == null) return null
            return BALANCE_REGEX.find(response)
                ?.groupValues?.get(1)
                ?.replace(",", "")
                ?.toDoubleOrNull()
        }
    }

    /**
     * Executes [code] on the SIM in [simSlot]. When [multiStep] is true the
     * code is split into steps and each is run sequentially; the run fails as
     * soon as one step fails.
     */
    suspend fun run(code: String, simSlot: Int, multiStep: Boolean = false): UssdResult {
        val steps = if (multiStep) {
            code.split(STEP_SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(code.trim())
        }
        if (steps.isEmpty()) return UssdResult(success = false)

        // Hold the lock for the whole sequence so a multi-step session is
        // never interleaved with another request.
        return ussdLock.withLock {
            var lastResponse: String? = null
            for ((index, step) in steps.withIndex()) {
                if (index > 0) delay(STEP_DELAY_MS)
                val result = runSingle(step, simSlot)
                if (!result.success) {
                    return@withLock UssdResult(success = false, response = result.response)
                }
                lastResponse = result.response
            }
            UssdResult(success = true, response = lastResponse)
        }
    }

    private suspend fun runSingle(code: String, simSlot: Int): UssdResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ussdApi26(code, simSlot)
        } else {
            UssdResult(success = dialIntent(code))
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun ussdApi26(code: String, simSlot: Int): UssdResult {
        val default = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val subId = SimManager.subscriptionIdForSlot(context, simSlot)
        val tm = if (subId != null) default.createForSubscriptionId(subId) else default

        return try {
            withTimeoutOrNull(USSD_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    tm.sendUssdRequest(
                        code,
                        object : TelephonyManager.UssdResponseCallback() {
                            override fun onReceiveUssdResponse(
                                tm: TelephonyManager, request: String, response: CharSequence
                            ) {
                                if (cont.isActive) cont.resume(UssdResult(true, response.toString()))
                            }

                            override fun onReceiveUssdResponseFailed(
                                tm: TelephonyManager, request: String, failureCode: Int
                            ) {
                                if (cont.isActive) cont.resume(UssdResult(false, "failure code $failureCode"))
                            }
                        },
                        Handler(Looper.getMainLooper())
                    )
                }
            } ?: UssdResult(success = false, response = "timeout")
        } catch (e: SecurityException) {
            UssdResult(success = false, response = e.message)
        }
    }

    private fun dialIntent(code: String): Boolean {
        return try {
            val uri = Uri.parse("tel:${Uri.encode(code)}")
            context.startActivity(
                Intent(Intent.ACTION_CALL, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Queries the SIM's airtime balance via [BALANCE_CODE]; null on failure. */
    suspend fun fetchAirtimeBalance(simSlot: Int): Double? {
        val result = ussdLock.withLock { runSingle(BALANCE_CODE, simSlot) }
        if (!result.success) return null
        return parseBalance(result.response)
    }
}
