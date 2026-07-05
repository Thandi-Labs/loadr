package com.bytethrux.loadr.data.repository

import com.bytethrux.loadr.data.local.TokenDataStore
import com.bytethrux.loadr.data.network.ApiService
import com.bytethrux.loadr.data.network.TransactionDto
import com.bytethrux.loadr.data.network.HomeStatsDto
import kotlinx.coroutines.flow.first

sealed class HomeResult<out T> {
    data class Success<T>(val data: T) : HomeResult<T>()
    data class Error(val message: String) : HomeResult<Nothing>()
}

class HomeRepository(
    private val api: ApiService,
    private val tokenDataStore: TokenDataStore
) {
    private suspend fun bearerToken(): String {
        val token = tokenDataStore.accessToken.first()
            ?: throw Exception("Not authenticated")
        return "Bearer $token"
    }

    suspend fun getStats(): HomeResult<HomeStatsDto> {
        // Keeping dashboard items hardcoded as requested
        return HomeResult.Success(mockStats())
    }

    suspend fun getRecentTransactions(): HomeResult<List<TransactionDto>> {
        return try {
            val txs = api.getTransactions(bearerToken())
            HomeResult.Success(txs)
        } catch (e: Exception) {
            HomeResult.Error(e.message ?: "Failed to fetch transactions from database")
        }
    }

    private fun mockStats() = HomeStatsDto(
        // successful/failed counts and airtime_used are replaced at runtime
        // with tallies computed from today's transactions; token_balance
        // with the remaining subscription tokens.
        successful_today = 0,
        failed_today = 0,
        token_balance = 0,
        airtime_used = 0.0,
        // Replaced at runtime with the real SIM balance read via *144#
        airtime_balance = 0.0,
        weekly_commission = 24.0,
        commission_by_day = listOf(2.0, 6.0, 6.0, 0.0, 0.0, 2.0, 8.0)
    )
}
