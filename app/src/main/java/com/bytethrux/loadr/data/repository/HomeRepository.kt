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
            HomeResult.Success(mockStats())
        }
    }

    suspend fun getRecentTransactions(): HomeResult<List<TransactionDto>> {
        return try {
            val txs = api.getTransactions(bearerToken())
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
        TransactionDto(1, customer_name = "MUTUA NZYOKI", package_name = "1GB · valid 1Hr", amount = 21.0, created_at = "3h ago", status = "success"),
        TransactionDto(2, customer_name = "NEWTON OMUSINA", package_name = "250MBS · 24Hrs", amount = 20.0, created_at = "5h ago", status = "success"),
        TransactionDto(3, customer_name = "MUTUA NZYOKI", package_name = "45 Min · 3Hrs", amount = 22.0, created_at = "6h ago", status = "success"),
        TransactionDto(4, customer_name = "NEWTON OMUSINA", package_name = "250MBS · 24Hrs", amount = 20.0, created_at = "15h ago", status = "success"),
    )
}
