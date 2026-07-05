package com.bytethrux.loadr.data.ussd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UssdBalanceParserTest {

    @Test
    fun `parses the real Safaricom star-144 reply with amount before KSH`() {
        val response = "Airtime Bal: 5.10 KSH. Expire on 30/09/2026."
        assertEquals(5.10, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `parses amount-before-KSH with thousands separator`() {
        val response = "Airtime Bal: 1,005.75 KSH. Expire on 30/09/2026."
        assertEquals(1005.75, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `parses amount-before-KSH without a Bal keyword`() {
        val response = "You have 20.00 KSH of airtime remaining"
        assertEquals(20.0, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `expiry date digits are not mistaken for the balance`() {
        val response = "Airtime Bal: 5.10 KSH. Expire 30/09/2026 23:59"
        assertEquals(5.10, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `parses standard Safaricom balance response`() {
        val response = "Your account balance is Ksh 284.29 valid until 31/12/2026."
        assertEquals(284.29, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `parses balance with thousands separator`() {
        val response = "Your account balance is Ksh1,024.50"
        assertEquals(1024.50, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `parses balance without decimals`() {
        val response = "balance is Ksh500"
        assertEquals(500.0, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `parses Kshs variant with dot`() {
        val response = "You have Kshs. 72.10 airtime"
        assertEquals(72.10, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `parses lowercase ksh`() {
        val response = "Acc balance: ksh 15.00"
        assertEquals(15.0, UssdExecutor.parseBalance(response)!!, 0.001)
    }

    @Test
    fun `returns null when response has no amount`() {
        assertNull(UssdExecutor.parseBalance("Request could not be completed"))
    }

    @Test
    fun `returns null for null response`() {
        assertNull(UssdExecutor.parseBalance(null))
    }

    @Test
    fun `uses the first amount when several are present`() {
        val response = "Balance Ksh 50.25. Bonus Ksh 10.00 expires soon."
        assertEquals(50.25, UssdExecutor.parseBalance(response)!!, 0.001)
    }
}
