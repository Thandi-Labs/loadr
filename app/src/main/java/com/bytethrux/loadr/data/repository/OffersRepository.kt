package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.OfferDto
import kotlinx.coroutines.flow.first

sealed class OffersResult<out T> {
    data class Success<T>(val data: T) : OffersResult<T>()
    data class Error(val message: String) : OffersResult<Nothing>()
}

class OffersRepository(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) {
    private suspend fun bearerToken(): String {
        val token = tokenDataStore.accessToken.first()
        return "Bearer $token"
    }

    suspend fun getOffers(): OffersResult<List<OfferDto>> {
        return try {
            val offers = api.getOffers(bearerToken())
            OffersResult.Success(offers)
        } catch (e: Exception) {
            OffersResult.Error(e.message ?: "Failed to load offers")
        }
    }

    suspend fun createOffer(offer: OfferDto): OffersResult<OfferDto> {
        return try {
            val created = api.createOffer(bearerToken(), offer)
            OffersResult.Success(created)
        } catch (e: Exception) {
            OffersResult.Error(e.message ?: "Failed to create offer")
        }
    }

    suspend fun updateOffer(offer: OfferDto): OffersResult<OfferDto> {
        return try {
            val updated = api.updateOffer(bearerToken(), offer.id, offer)
            OffersResult.Success(updated)
        } catch (e: Exception) {
            OffersResult.Error(e.message ?: "Failed to update offer")
        }
    }

    suspend fun deleteOffer(id: Int): OffersResult<Unit> {
        return try {
            api.deleteOffer(bearerToken(), id)
            OffersResult.Success(Unit)
        } catch (e: Exception) {
            OffersResult.Error(e.message ?: "Failed to delete offer")
        }
    }
}
