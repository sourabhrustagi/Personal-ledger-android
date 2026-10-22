package com.ledger.personal.data.tools

import com.ledger.personal.core.LedgerConstants
import com.ledger.personal.domain.model.Transaction
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Statement summary. */
data class StatementSummary(val month: YearMonth, val rows: List<Transaction>, val inMinor: Long, val outMinor: Long)

/** Statement for. */
fun statementFor(all: List<Transaction>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): StatementSummary {
    val rows = all
        .filter { tx ->
            tx.currency == LedgerConstants.DEFAULT_CURRENCY &&
                YearMonth.from(Instant.ofEpochMilli(tx.timestampEpochMs).atZone(zone)) == month
        }
        .sortedBy { it.timestampEpochMs }
    val credit = rows.filter { it.category == LedgerConstants.CATEGORY_SALARY }
    return StatementSummary(
        month = month,
        rows = rows,
        inMinor = credit.sumOf { it.amountInCents },
        outMinor = (rows - credit.toSet()).sumOf { it.amountInCents }
    )
}
