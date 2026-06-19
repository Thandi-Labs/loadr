package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import kotlinx.coroutines.flow.Flow

sealed class AuthResult {
    data object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) {
    val savedToken: Flow<String?> = tokenDataStore.accessToken
    val savedUsername: Flow<String?> = tokenDataStore.username

    suspend fun login(username: String, password: String): AuthResult {
        return try {
            val response = api.login(username, password)
            tokenDataStore.saveToken(response.access_token)
            tokenDataStore.saveUsername(username)
            AuthResult.Success
        } catch (e: retrofit2.HttpException) {
            val message = when (e.code()) {
                401 -> "Invalid username or password"
                422 -> "Please check your credentials"
                else -> "Server error (${e.code()})"
            }
            AuthResult.Error(message)
        } catch (e: java.net.ConnectException) {
            AuthResult.Error("Cannot reach server. Check your connection.")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "An unexpected error occurred")
        }
    }

    suspend fun logout() {
        tokenDataStore.clearToken()
    }
}