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
        return "Bearer $token"
    }

    suspend fun getStats(): HomeResult<HomeStatsDto> {
        return try {
            val stats = api.getHomeStats(bearerToken())
            HomeResult.Success(stats)
        } catch (e: Exception) {
            // Return mock data while backend endpoint isn't built yet
            HomeResult.Success(mockStats())
        }
    }

    suspend fun getRecentTransactions(): HomeResult<List<TransactionDto>> {
        return try {
            val txs = api.getRecentTransactions(bearerToken())
            HomeResult.Success(txs)
        } catch (e: Exception) {
            HomeResult.Success(mockTransactions())
        }
    }

    private fun mockStats() = HomeStatsDto(
        successful_today = 3,
        failed_today = 0,
        token_balance = 518,
        airtime_used = 65.0,
        airtime_balance = 284.29,
        weekly_commission = 24.0,
        commission_by_day = listOf(2.0, 6.0, 6.0, 0.0, 0.0, 2.0, 8.0)
    )

    private fun mockTransactions() = listOf(
        TransactionDto(1, "MUTUA NZYOKI", "1GB · valid 1Hr", 21, "3h ago", "success"),
        TransactionDto(2, "NEWTON OMUSINA", "250MBS · 24Hrs", 20, "5h ago", "success"),
        TransactionDto(3, "MUTUA NZYOKI", "45 Min · 3Hrs", 22, "6h ago", "success"),
        TransactionDto(4, "NEWTON OMUSINA", "250MBS · 24Hrs", 20, "15h ago", "success"),
    )
}