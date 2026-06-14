package com.bytethrux.loadr.data.network


import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import  retrofit2.http.GET
import retrofit2.http.Header
data class TokenResponse(
    val access_token: String,
    val token_type: String
)

data class TransactionDto(
    val id: Int,
    val customer_name: String,
    val package_name: String,
    val amount: Int,
    val created_at: String,
    val status: String       // "success" | "failed"
)

data class HomeStatsDto(
    val successful_today: Int,
    val failed_today: Int,
    val token_balance: Int,
    val airtime_used: Double,
    val airtime_balance: Double,
    val weekly_commission: Double,
    val commission_by_day: List<Double>  // 7 values, Sun→Sat
)

interface ApiService {

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse

    @GET("transactions/today")
    suspend fun getRecentTransactions(
        @Header("Authorization") token: String
    ): List<TransactionDto>

    @GET("home/stats")
    suspend fun getHomeStats(
        @Header("Authorization") token: String
    ): HomeStatsDto
}


