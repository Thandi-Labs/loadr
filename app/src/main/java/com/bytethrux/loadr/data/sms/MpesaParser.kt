package com.bytethrux.loadr.data.sms

data class MpesaPayment(
    val transactionCode: String,
    val amount: Double,
    val senderName: String,
    val senderPhone: String,
)

object MpesaParser {

    // M-Pesa "received" messages:
    // "BK54321 Confirmed.\nYou have received Ksh500 from JOHN DOE 254712345678 on ..."
    // "OEI5K5BC3I Confirmed. Ksh1,200 received from ALICE WANJIKU 0712345678 on ..."

    private val transactionCodeRegex = Regex("""^([A-Z0-9]{6,12})\s+Confirmed""")
    private val amountRegex = Regex("""Ksh([\d,]+)""")
    private val senderRegex = Regex("""from ([A-Z][A-Z ]+?)\s+(254\d{9}|0\d{9})""")

    fun parse(message: String): MpesaPayment? {
        if (!message.contains("received", ignoreCase = true)) return null

        val code = transactionCodeRegex.find(message)?.groupValues?.get(1) ?: return null

        val amountStr = amountRegex.find(message)?.groupValues?.get(1)
            ?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null

        val senderMatch = senderRegex.find(message.uppercase()) ?: return null
        val senderName = senderMatch.groupValues[1].trim()
        val rawPhone = senderMatch.groupValues[2]

        return MpesaPayment(
            transactionCode = code,
            amount = amount,
            senderName = senderName,
            senderPhone = normalizePhone(rawPhone),
        )
    }

    // Normalise to local 07/01 format so USSD replacement is consistent
    private fun normalizePhone(raw: String): String = when {
        raw.startsWith("254") -> "0${raw.substring(3)}"
        else -> raw
    }
}
