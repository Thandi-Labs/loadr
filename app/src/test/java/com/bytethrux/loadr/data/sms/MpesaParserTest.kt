package com.bytethrux.loadr.data.sms

import org.junit.Assert.*
import org.junit.Test

class MpesaParserTest {

    // ------------------------------------------------------------------
    // Happy-path: full valid messages
    // ------------------------------------------------------------------

    @Test
    fun `parse valid message with local 07 phone returns payment`() {
        val msg = "BK54321 Confirmed.\nYou have received Ksh500 from JOHN DOE 0712345678 on 1/1/24"
        val result = MpesaParser.parse(msg)
        assertNotNull(result)
        assertEquals("BK54321", result!!.transactionCode)
        assertEquals(500.0, result.amount, 0.001)
        assertEquals("JOHN DOE", result.senderName)
        assertEquals("0712345678", result.senderPhone)
    }

    @Test
    fun `parse valid message with 254 international phone normalizes to 0 prefix`() {
        val msg = "OEI5K5BC3I Confirmed. Ksh1,200 received from ALICE WANJIKU 254712345678 on 2/6/24"
        val result = MpesaParser.parse(msg)
        assertNotNull(result)
        assertEquals("OEI5K5BC3I", result!!.transactionCode)
        assertEquals(1200.0, result.amount, 0.001)
        assertEquals("ALICE WANJIKU", result.senderName)
        assertEquals("0712345678", result.senderPhone)
    }

    @Test
    fun `parse amount with comma separators is parsed correctly`() {
        val msg = "ABC123456 Confirmed. Ksh10,500 received from JAMES MWANGI 0722111222 on 3/3/24"
        val result = MpesaParser.parse(msg)
        assertNotNull(result)
        assertEquals(10500.0, result!!.amount, 0.001)
    }

    @Test
    fun `parse whole-number amount without commas works`() {
        val msg = "XY98765432 Confirmed. Ksh100 received from MARY NJERI 0700123456 on 5/5/24"
        val result = MpesaParser.parse(msg)
        assertNotNull(result)
        assertEquals(100.0, result!!.amount, 0.001)
    }

    @Test
    fun `parse case-insensitive received keyword`() {
        val msg = "BK54321 Confirmed. You have RECEIVED Ksh500 from JOHN DOE 0712345678 on 1/1/24"
        assertNotNull(MpesaParser.parse(msg))
    }

    // ------------------------------------------------------------------
    // Phone normalisation
    // ------------------------------------------------------------------

    @Test
    fun `254 prefix is stripped and replaced with 0`() {
        val msg = "TX123ABC Confirmed. Ksh200 received from PETER KAMAU 254733000111 on 6/6/24"
        val result = MpesaParser.parse(msg)
        assertEquals("0733000111", result!!.senderPhone)
    }

    @Test
    fun `local 0 prefix phone is unchanged`() {
        val msg = "TX123ABC Confirmed. Ksh200 received from PETER KAMAU 0733000111 on 6/6/24"
        val result = MpesaParser.parse(msg)
        assertEquals("0733000111", result!!.senderPhone)
    }

    // ------------------------------------------------------------------
    // Null cases: missing required fields
    // ------------------------------------------------------------------

    @Test
    fun `parse returns null when message has no received keyword`() {
        val msg = "BK54321 Confirmed. Ksh500 sent to JOHN DOE 0712345678 on 1/1/24"
        assertNull(MpesaParser.parse(msg))
    }

    @Test
    fun `parse returns null for empty string`() {
        assertNull(MpesaParser.parse(""))
    }

    @Test
    fun `parse returns null when transaction code is missing`() {
        val msg = "You have received Ksh500 from JOHN DOE 0712345678 on 1/1/24"
        assertNull(MpesaParser.parse(msg))
    }

    @Test
    fun `parse returns null when amount is missing`() {
        val msg = "BK54321 Confirmed. You have received from JOHN DOE 0712345678 on 1/1/24"
        assertNull(MpesaParser.parse(msg))
    }

    @Test
    fun `parse returns null when sender info is missing`() {
        val msg = "BK54321 Confirmed. You have received Ksh500 on 1/1/24"
        assertNull(MpesaParser.parse(msg))
    }

    @Test
    fun `parse returns null when transaction code is not at message start`() {
        // Regex is anchored with ^ so code must start the message
        val msg = "Prefix BK54321 Confirmed. Ksh500 received from JOHN DOE 0712345678 on 1/1/24"
        assertNull(MpesaParser.parse(msg))
    }

    @Test
    fun `parse returns null for unrelated bank message`() {
        val msg = "Your Equity Bank account has been credited with KES 5000"
        assertNull(MpesaParser.parse(msg))
    }
}
