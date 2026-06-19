package com.bytethrux.loadr.data.network


import com.google.gson.annotations.SerializedName
import retrofit2.http.*

data class TokenResponse(
    val access_token: String,
    val token_type: String
)

data class TransactionDto(
    val id: Int,
    val user_id: Int? = null,
    val offer_id: Int? = null,
    val customer_name: String,
    val customer_phone: String? = null,
    val package_name: String? = null,
    val amount: Double,
    val status: String,       // "success" | "failed"
    val created_at: String
)

data class HomeStatsDto(
    val successful_today: Int,
    val failed_today: Int,
    val token_balance: Int,
    val airtime_used: Double,
    val airtime_balance: Double,
    val weekly_commission: Double,
    val commission_by_day: List<Double>
)

enum class OfferType {
    @SerializedName("data") DATA,
    @SerializedName("minutes") MINUTES,
    @SerializedName("sms") SMS
}

data class OfferDto(
    val id: Int,
    val offer_name: String,
    val ussd: String,
    val amount: Double,
    val active: Boolean = true,
    val category: OfferType,
    val user_id: Int? = null
)

data class CreateTransactionRequest(
    val offer_id: Int,
    val customer_name: String,
    val customer_phone: String,
    val amount: Int,
    val status: String,       // "success" | "failed"
    val created_at: String,   // "YYYY-MM-DD"
)

interface ApiService {

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("transactions")
    suspend fun getTransactions(
        @Header("Authorization") token: String
    ): List<TransactionDto>

    @GET("home/stats")
    suspend fun getHomeStats(
        @Header("Authorization") token: String
    ): HomeStatsDto

    @GET("offers/")
    suspend fun getOffers(
        @Header("Authorization") token: String
    ): List<OfferDto>

    @POST("offers/create")
    suspend fun createOffer(
        @Header("Authorization") token: String,
        @Body offer: OfferDto
    )

    @PUT("offers/update/{id}")
    suspend fun updateOffer(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body offer: OfferDto
    )

    @PUT("offers/deactivate/{id}")
    suspend fun deactivateOffer(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    )

    @DELETE("offers/delete/{id}")
    suspend fun deleteOffer(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    )

    @POST("transactions/create-transaction")
    suspend fun createTransaction(
        @Header("Authorization") token: String,
        @Body transaction: CreateTransactionRequest
    )
}


