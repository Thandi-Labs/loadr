package com.bytethrux.loadr.data.network


import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

data class TokenResponse(
    val access_token: String,
    val token_type: String
)

interface ApiService {

    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): TokenResponse
}