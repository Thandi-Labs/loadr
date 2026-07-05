package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.SubscriptionState
import com.bytethrux.loadr.data.local.SubscriptionStore
import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.SubscriptionPlanDto
import kotlinx.coroutines.flow.first
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Talks to the backend subscriptions API. The backend owns plans and the
 * user's entitlement; [SubscriptionStore] is kept in sync as an offline cache
 * so payment gating still works without connectivity.
 */
class SubscriptionsRepository(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore,
    private val subscriptionStore: SubscriptionStore,
) {

    companion object {
        // FastAPI serialises datetimes as "2026-08-04T07:52:11.123456".
        private const val ISO_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"

        fun parseExpiry(isoDateTime: String): Long? {
            val trimmed = isoDateTime.take(ISO_PATTERN.count { it != '\'' })
            return try {
                SimpleDateFormat(ISO_PATTERN, Locale.US).parse(trimmed)?.time
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun getPlans(): HomeResult<List<SubscriptionPlanDto>> {
        return try {
            HomeResult.Success(api.getSubscriptionPlans().filter { it.active })
        } catch (e: HttpException) {
            HomeResult.Error("Could not load subscription plans (HTTP ${e.code()})")
        } catch (e: Exception) {
            HomeResult.Error(e.message ?: "Could not load subscription plans")
        }
    }

    /**
     * Fetches the active user subscription and mirrors it into the local
     * cache. A 404 (no active subscription) clears the cache; network or
     * auth failures leave the cached entitlement untouched.
     */
    suspend fun syncMySubscription(): SubscriptionState {
        val token = tokenDataStore.accessToken.first()
            ?: return subscriptionStore.current()

        return try {
            val sub = api.getMySubscription("Bearer $token")
            val expiryAt = parseExpiry(sub.expiry_date)
            if (expiryAt != null) {
                subscriptionStore.setFromBackend(expiryAt, sub.requests_remaining)
            }
            subscriptionStore.current()
        } catch (e: HttpException) {
            if (e.code() == 404) subscriptionStore.clearEntitlement()
            subscriptionStore.current()
        } catch (_: Exception) {
            // Offline — fall back to the cached entitlement.
            subscriptionStore.current()
        }
    }

    /** Records a purchase on the backend after the activation USSD succeeds. */
    suspend fun purchase(subscriptionId: Int): HomeResult<Unit> {
        val token = tokenDataStore.accessToken.first()
            ?: return HomeResult.Error("Not authenticated")
        return try {
            api.purchaseSubscription("Bearer $token", subscriptionId)
            HomeResult.Success(Unit)
        } catch (e: HttpException) {
            val detail = try {
                e.response()?.errorBody()?.string()
                    ?.substringAfter("\"detail\":\"")?.substringBefore("\"")
                    ?.takeIf { it.isNotBlank() && !it.startsWith("{") }
            } catch (_: Exception) {
                null
            }
            HomeResult.Error(detail ?: "Purchase failed (${e.code()})")
        } catch (e: Exception) {
            HomeResult.Error(e.message ?: "Purchase failed")
        }
    }
}
