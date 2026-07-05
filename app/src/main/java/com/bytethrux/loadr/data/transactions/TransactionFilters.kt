package com.bytethrux.loadr.data.transactions

import com.bytethrux.loadr.data.network.TransactionDto
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class StatusFilter(val label: String, private val status: String?) {
    LIVE("Live", null),
    ALL("All", null),
    SUCCESSFUL("Successful", "success"),
    AWAITING_COMMISSION("Awaiting Commission", "awaiting_commission"),
    FAILED("Failed", "failed"),
    PENDING("Pending", "pending"),
    UNMATCHED("Unmatched", "unmatched"),
    SCHEDULED("Scheduled", "scheduled");

    fun matches(tx: TransactionDto): Boolean =
        status == null || tx.status.equals(status, ignoreCase = true)

    /** "No successful transactions" style empty-state message. */
    val emptyMessage: String
        get() = when (this) {
            LIVE, ALL -> "No transactions"
            else -> "No ${label.lowercase()} transactions"
        }
}

enum class DateFilter(val label: String, val days: Int?) {
    TODAY("Today", 1),
    YESTERDAY("Yesterday", 2),
    LAST_7_DAYS("Last 7 days", 7),
    LAST_30_DAYS("Last 30 days", 30),
    ALL_TIME("All time", null);
}

object TransactionFilters {

    private const val DATE_PATTERN = "yyyy-MM-dd"

    /**
     * Applies status, date and free-text filters. [todayMillis] is injectable
     * for tests; transactions with unparseable dates only survive "All time".
     */
    fun apply(
        transactions: List<TransactionDto>,
        status: StatusFilter,
        date: DateFilter,
        query: String = "",
        todayMillis: Long = System.currentTimeMillis(),
    ): List<TransactionDto> {
        val trimmedQuery = query.trim()
        return transactions.filter { tx ->
            status.matches(tx) &&
                matchesDate(tx, date, todayMillis) &&
                matchesQuery(tx, trimmedQuery)
        }
    }

    private fun matchesDate(tx: TransactionDto, filter: DateFilter, todayMillis: Long): Boolean {
        val days = filter.days ?: return true
        val txDay = parseDay(tx.created_at) ?: return false

        val today = startOfDay(todayMillis)
        val windowStart = today - (days - 1) * DAY_MS
        return when (filter) {
            DateFilter.TODAY -> txDay == today
            DateFilter.YESTERDAY -> txDay == today - DAY_MS
            else -> txDay in windowStart..today
        }
    }

    private fun matchesQuery(tx: TransactionDto, query: String): Boolean {
        if (query.isEmpty()) return true
        return tx.customer_name.contains(query, ignoreCase = true) ||
            (tx.customer_phone?.contains(query, ignoreCase = true) ?: false) ||
            (tx.package_name?.contains(query, ignoreCase = true) ?: false)
    }

    private const val DAY_MS = 24 * 60 * 60 * 1000L

    private fun parseDay(createdAt: String): Long? {
        val datePart = createdAt.take(DATE_PATTERN.length)
        return try {
            SimpleDateFormat(DATE_PATTERN, Locale.US).parse(datePart)?.time
        } catch (_: Exception) {
            null
        }
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Normalize through the same formatter used for parsing so timezone
        // handling stays consistent with parseDay().
        val formatted = SimpleDateFormat(DATE_PATTERN, Locale.US).format(cal.time)
        return SimpleDateFormat(DATE_PATTERN, Locale.US).parse(formatted)!!.time
    }
}
