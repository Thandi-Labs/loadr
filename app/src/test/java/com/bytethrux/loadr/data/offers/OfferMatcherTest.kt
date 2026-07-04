package com.bytethrux.loadr.data.offers

import com.bytethrux.loadr.data.network.OfferDto
import com.bytethrux.loadr.data.network.OfferType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferMatcherTest {

    private fun offer(
        id: Int,
        amount: Double,
        active: Boolean = true,
        ussd: String = "*180*5*2*LD*1#",
    ) = OfferDto(
        id = id,
        offer_name = "Offer $id",
        ussd = ussd,
        amount = amount,
        active = active,
        category = OfferType.DATA,
    )

    @Test
    fun `matches the offer whose amount equals the payment`() {
        val offers = listOf(offer(1, 20.0), offer(2, 50.0), offer(3, 100.0))
        assertEquals(2, OfferMatcher.match(offers, 50.0)!!.id)
    }

    @Test
    fun `matches within cents tolerance`() {
        val offers = listOf(offer(1, 99.99))
        assertEquals(1, OfferMatcher.match(offers, 100.0 - 0.005)!!.id)
    }

    @Test
    fun `skips inactive offers even when the amount matches`() {
        val offers = listOf(offer(1, 50.0, active = false), offer(2, 50.0))
        assertEquals(2, OfferMatcher.match(offers, 50.0)!!.id)
    }

    @Test
    fun `returns the first match when several offers share an amount`() {
        val offers = listOf(offer(7, 50.0), offer(8, 50.0))
        assertEquals(7, OfferMatcher.match(offers, 50.0)!!.id)
    }

    @Test
    fun `returns null when no offer matches the amount`() {
        val offers = listOf(offer(1, 20.0), offer(2, 50.0))
        assertNull(OfferMatcher.match(offers, 35.0))
    }

    @Test
    fun `returns null for an empty offer list`() {
        assertNull(OfferMatcher.match(emptyList(), 50.0))
    }

    @Test
    fun `buildUssd substitutes the customer phone for the LD placeholder`() {
        val result = OfferMatcher.buildUssd(offer(1, 50.0), "0712345678")
        assertEquals("*180*5*2*0712345678*1#", result)
    }

    @Test
    fun `buildUssd leaves codes without a placeholder untouched`() {
        val result = OfferMatcher.buildUssd(offer(1, 50.0, ussd = "*544*7#"), "0712345678")
        assertEquals("*544*7#", result)
    }

    @Test
    fun `buildUssd replaces every occurrence of the placeholder`() {
        val result = OfferMatcher.buildUssd(
            offer(1, 50.0, ussd = "*100*LD#;*200*LD#"), "0712345678"
        )
        assertEquals("*100*0712345678#;*200*0712345678#", result)
    }
}
