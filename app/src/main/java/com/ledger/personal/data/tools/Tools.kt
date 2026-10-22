package com.ledger.personal.data.tools

import com.ledger.personal.core.FieldMerge
import com.ledger.personal.core.mergeField
import com.ledger.personal.domain.model.Transaction
import java.net.URLDecoder
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/** Field diff. */
data class FieldDiff(val field: String, val mine: String, val theirs: String)

/** Conflict fields. */
fun conflictFields(local: Transaction, server: Transaction): List<FieldDiff> = listOfNotNull(
    fieldDiff("Description", local.description, server.description),
    fieldDiff("Category", local.category, server.category),
    fieldDiff("Amount (minor units)", local.amountInCents.toString(), server.amountInCents.toString()),
)

private fun fieldDiff(field: String, mine: String, theirs: String): FieldDiff? =
    (mergeField(mine, theirs) as? FieldMerge.Conflict)?.let { FieldDiff(field, it.left, it.right) }

/** Builds the merged record from per-field choices; `true` keeps this device's value. */
fun resolveConflict(local: Transaction, server: Transaction, keepMine: Map<String, Boolean>): Transaction = server.copy(
    description = if (keepMine["Description"] == true) local.description else server.description,
    category = if (keepMine["Category"] == true) local.category else server.category,
    amountInCents = if (keepMine["Amount (minor units)"] == true) local.amountInCents else server.amountInCents,
)

/** Upi target. */
data class UpiTarget(val vpa: String, val name: String?, val amountMinor: Long)

private val vpaRegex = Regex("^[^\\s/@]+@[^\\s/@]+$")

/** Parses upi. */
fun parseUpi(raw: String): UpiTarget? {
    val text = raw.trim()
    if (text.startsWith("upi://pay?", ignoreCase = true)) {
        val params = text.substringAfter('?').split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=', "")
            if (key.isEmpty()) null else key.lowercase() to URLDecoder.decode(pair.substringAfter("="), "UTF-8")
        }.toMap()
        val vpa = params["pa"]?.takeIf { it.contains('@') } ?: return null
        val amount = params["am"]?.let { com.ledger.personal.ui.feature.parseMinor(it) } ?: 0L
        return UpiTarget(vpa, params["pn"], amount)
    }
    if (vpaRegex.matches(text)) {
        return UpiTarget(text, null, 0L)
    }
    return null
}

/** Statement summary. */
data class StatementSummary(val month: YearMonth, val rows: List<Transaction>, val inMinor: Long, val outMinor: Long)

/** Statement for. */
fun statementFor(all: List<Transaction>, month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): StatementSummary {
    val rows = all.filter { it.currency == "INR" && YearMonth.from(Instant.ofEpochMilli(it.timestampEpochMs).atZone(zone)) == month }
        .sortedBy { it.timestampEpochMs }
    val credit = rows.filter { it.category == "Salary" }
    return StatementSummary(month, rows, credit.sumOf { it.amountInCents }, (rows - credit.toSet()).sumOf { it.amountInCents })
}
