package com.bytethrux.loadr.data.transactions

import com.bytethrux.loadr.data.network.TransactionDto
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionFiltersTest {

    // Fixed "today" for deterministic date filtering.
    private val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("2026-07-05")!!.time

    private fun tx(
        id: Int,
        status: String = "success",
        createdAt: String = "2026-07-05",
        name: String = "JOHN DOE",
        phone: String? = "0712345678",
        pkg: String? = "1GB Bundle",
    ) = TransactionDto(
        id = id,
        customer_name = name,
        customer_phone = phone,
        package_name = pkg,
        amount = 100.0,
        status = status,
        created_at = createdAt,
    )

    private fun ids(list: List<TransactionDto>) = list.map { it.id }

    @Test
    fun `successful filter keeps only success status`() {
        val txs = listOf(tx(1, "success"), tx(2, "failed"), tx(3, "success"))
        val result = TransactionFilters.apply(txs, StatusFilter.SUCCESSFUL, DateFilter.ALL_TIME, todayMillis = today)
        assertEquals(listOf(1, 3), ids(result))
    }

    @Test
    fun `failed filter keeps only failed status`() {
        val txs = listOf(tx(1, "success"), tx(2, "failed"))
        val result = TransactionFilters.apply(txs, StatusFilter.FAILED, DateFilter.ALL_TIME, todayMillis = today)
        assertEquals(listOf(2), ids(result))
    }

    @Test
    fun `all and live filters keep every status`() {
        val txs = listOf(tx(1, "success"), tx(2, "failed"), tx(3, "pending"))
        assertEquals(3, TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.ALL_TIME, todayMillis = today).size)
        assertEquals(3, TransactionFilters.apply(txs, StatusFilter.LIVE, DateFilter.ALL_TIME, todayMillis = today).size)
    }

    @Test
    fun `today filter keeps only transactions dated today`() {
        val txs = listOf(tx(1, createdAt = "2026-07-05"), tx(2, createdAt = "2026-07-04"))
        val result = TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.TODAY, todayMillis = today)
        assertEquals(listOf(1), ids(result))
    }

    @Test
    fun `yesterday filter keeps only yesterday`() {
        val txs = listOf(tx(1, createdAt = "2026-07-05"), tx(2, createdAt = "2026-07-04"), tx(3, createdAt = "2026-07-03"))
        val result = TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.YESTERDAY, todayMillis = today)
        assertEquals(listOf(2), ids(result))
    }

    @Test
    fun `last 7 days is a rolling window including today`() {
        val txs = listOf(
            tx(1, createdAt = "2026-07-05"),
            tx(2, createdAt = "2026-06-29"),
            tx(3, createdAt = "2026-06-28"),
        )
        val result = TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.LAST_7_DAYS, todayMillis = today)
        assertEquals(listOf(1, 2), ids(result))
    }

    @Test
    fun `unparseable dates only survive all time`() {
        val txs = listOf(tx(1, createdAt = "yesterday-ish"))
        assertEquals(0, TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.TODAY, todayMillis = today).size)
        assertEquals(1, TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.ALL_TIME, todayMillis = today).size)
    }

    @Test
    fun `query matches name phone or package case-insensitively`() {
        val txs = listOf(
            tx(1, name = "PAUL ONCHOKE"),
            tx(2, phone = "0790797274"),
            tx(3, pkg = "Weekly Minutes"),
            tx(4, name = "JANE", phone = null, pkg = null),
        )
        assertEquals(listOf(1), ids(TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.ALL_TIME, "paul", today)))
        assertEquals(listOf(2), ids(TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.ALL_TIME, "0790", today)))
        assertEquals(listOf(3), ids(TransactionFilters.apply(txs, StatusFilter.ALL, DateFilter.ALL_TIME, "minutes", today)))
    }

    @Test
    fun `status and date filters combine`() {
        val txs = listOf(
            tx(1, "success", "2026-07-05"),
            tx(2, "failed", "2026-07-05"),
            tx(3, "success", "2026-07-01"),
        )
        val result = TransactionFilters.apply(txs, StatusFilter.SUCCESSFUL, DateFilter.TODAY, todayMillis = today)
        assertEquals(listOf(1), ids(result))
    }

    // ------------------------------------------------------------------
    // airtimeUsedToday()
    // ------------------------------------------------------------------

    @Test
    fun `airtime used today sums today's successful transactions`() {
        val txs = listOf(
            tx(1, "success", "2026-07-05"),
            tx(2, "success", "2026-07-05"),
            tx(3, "success", "2026-07-04"),   // yesterday — excluded
        )
        assertEquals(200.0, TransactionFilters.airtimeUsedToday(txs, today), 0.001)
    }

    @Test
    fun `failed transactions consume no airtime`() {
        val txs = listOf(
            tx(1, "success", "2026-07-05"),
            tx(2, "failed", "2026-07-05"),
        )
        assertEquals(100.0, TransactionFilters.airtimeUsedToday(txs, today), 0.001)
    }

    @Test
    fun `airtime used today is zero with no transactions today`() {
        val txs = listOf(tx(1, "success", "2026-07-01"))
        assertEquals(0.0, TransactionFilters.airtimeUsedToday(txs, today), 0.001)
        assertEquals(0.0, TransactionFilters.airtimeUsedToday(emptyList(), today), 0.001)
    }

    @Test
    fun `empty message names the active filter`() {
        assertEquals("No successful transactions", StatusFilter.SUCCESSFUL.emptyMessage)
        assertEquals("No failed transactions", StatusFilter.FAILED.emptyMessage)
        assertEquals("No transactions", StatusFilter.ALL.emptyMessage)
    }
}
