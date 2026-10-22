package com.ledger.personal.data.tools

import com.ledger.personal.domain.model.Transaction
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Statement summary. */
data class StatementSummary(val month: YearMonth, val rows: List<Transaction>, val inMinor: Long, val outMinor: Long)

/** Statement for. */
fun statementFor(all: List<Transaction>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): StatementSummary {
    val rows = all.filter { it.currency == "INR" && YearMonth.from(Instant.ofEpochMilli(it.timestampEpochMs).atZone(zone)) == month }
        .sortedBy { it.timestampEpochMs }
    val credit = rows.filter { it.category == "Salary" }
    return StatementSummary(month, rows, credit.sumOf { it.amountInCents }, (rows - credit.toSet()).sumOf { it.amountInCents })
}
