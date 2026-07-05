package com.bytethrux.loadr.service

import com.bytethrux.loadr.data.local.LoadrSettings
import com.bytethrux.loadr.data.local.ProcessingMode
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.CreateTransactionRequest
import com.bytethrux.loadr.data.network.OfferDto
import com.bytethrux.loadr.data.offers.OfferMatcher
import com.bytethrux.loadr.data.repository.SubscriptionsRepository
import com.bytethrux.loadr.data.ussd.UssdExecutor
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The SMS → offer → USSD → DB pipeline, extracted from [LoadrService] so
 * every step can be exercised with mocks:
 *
 *  1. Gate on the subscription entitlement (active window + requests left).
 *  2. Iterate the offers until one matches the paid amount.
 *  3. Replace the "LD" portion of the offer's USSD with the customer phone.
 *  4. Run the USSD on the Bingwa SIM.
 *  5. On success: consume one token (1 token per USSD) and record the
 *     transaction in the DB. Failed USSDs are recorded too, with status
 *     "failed", but do not consume a token.
 */
class PaymentProcessor(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
    private val subscriptionStore: SubscriptionStore,
    private val subscriptionsRepository: SubscriptionsRepository,
    private val ussdExecutor: UssdExecutor,
) {

    sealed class Outcome {
        object NoEntitlement : Outcome()
        object NotSignedIn : Outcome()
        data class OffersUnavailable(val reason: String?) : Outcome()
        data class NoMatchingOffer(val amount: Double) : Outcome()
        data class Sent(val offer: OfferDto) : Outcome()
        data class UssdFailed(val offer: OfferDto, val reason: String?) : Outcome()
    }

    suspend fun process(
        amount: Double,
        phone: String,
        customerName: String,
        settings: LoadrSettings,
        onSending: (offerName: String) -> Unit = {},
    ): Outcome {
        // 1. Entitlement gate — synced from the backend, cached offline.
        val entitlement = subscriptionsRepository.syncMySubscription()
        if (!entitlement.hasCapability()) return Outcome.NoEntitlement

        val token = tokenDataStore.accessToken.first() ?: return Outcome.NotSignedIn
        val bearer = "Bearer $token"

        // 2. Find the offer whose price matches the paid amount.
        val offers = try {
            api.getOffers(bearer)
        } catch (e: Exception) {
            return Outcome.OffersUnavailable(e.message)
        }
        val offer = OfferMatcher.match(offers, amount)
            ?: return Outcome.NoMatchingOffer(amount)

        // 3. Substitute the customer's phone number for the LD placeholder.
        val ussdCode = OfferMatcher.buildUssd(offer, phone)
        onSending(offer.offer_name)

        // 4. Run the USSD on the configured Bingwa SIM.
        val result = ussdExecutor.run(
            code = ussdCode,
            simSlot = settings.bingwaSimSlot,
            multiStep = settings.processingMode == ProcessingMode.ADVANCED,
        )

        // 5. Tokens and the transaction record.
        if (result.success) subscriptionStore.consumeToken()

        try {
            api.createTransaction(
                bearer,
                CreateTransactionRequest(
                    offer_id = offer.id,
                    customer_name = customerName,
                    customer_phone = phone,
                    amount = amount.toInt(),
                    status = if (result.success) "success" else "failed",
                    created_at = todayDate(),
                )
            )
        } catch (_: Exception) {
            // Recording must never undo an executed USSD; the backend can
            // reconcile from the notification trail.
        }

        return if (result.success) Outcome.Sent(offer)
        else Outcome.UssdFailed(offer, result.response)
    }

    private fun todayDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
}
