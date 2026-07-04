package com.bytethrux.loadr.data.offers

import com.bytethrux.loadr.data.network.OfferDto
import kotlin.math.abs

/**
 * Pure logic for turning a received payment into a USSD request: iterate the
 * offers until one matches the paid amount, then substitute the customer's
 * phone number into the offer's USSD template.
 */
object OfferMatcher {

    /** The placeholder in offer USSD templates that stands for the customer phone. */
    const val PHONE_PLACEHOLDER = "LD"

    // Amounts compare with a small tolerance to absorb cents rounding.
    private const val AMOUNT_TOLERANCE = 0.01

    /** First active offer whose price matches [amount], or null. */
    fun match(offers: List<OfferDto>, amount: Double): OfferDto? =
        offers.firstOrNull { it.active && abs(it.amount - amount) < AMOUNT_TOLERANCE }

    /** The offer's USSD code with [PHONE_PLACEHOLDER] replaced by [phone]. */
    fun buildUssd(offer: OfferDto, phone: String): String =
        offer.ussd.replace(PHONE_PLACEHOLDER, phone)
}
