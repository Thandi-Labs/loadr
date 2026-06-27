package com.bytethrux.loadr.data.network

import com.bytethrux.loadr.data.local.TokenDataStore
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    private const val BASE_URL = "https://loadrd-fastapi.onrender.com/"

    private lateinit var tokenDataStore: TokenDataStore

    fun initialize(dataStore: TokenDataStore) {
        if (!::tokenDataStore.isInitialized) {
            tokenDataStore = dataStore
        }
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val instance: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (response.code == 401 && ::tokenDataStore.isInitialized) {
                    val requestToken = request.header("Authorization")?.removePrefix("Bearer ")
                    val currentToken = runBlocking { tokenDataStore.accessToken.first() }
                    if (requestToken != null && requestToken == currentToken) {
                        runBlocking { tokenDataStore.clearToken() }
                    }
                }
                response
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
