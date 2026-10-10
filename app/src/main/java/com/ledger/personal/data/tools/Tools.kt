package com.ledger.personal.data.tools

import android.content.Context
import com.ledger.personal.core.FieldMerge
import com.ledger.personal.core.mergeField
import com.ledger.personal.domain.model.Transaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

/** Allowed values of dispute reason. */
enum class DisputeReason(val label: String) {
    UNAUTHORISED("I didn't make this payment"),
    WRONG_AMOUNT("I was charged the wrong amount"),
    NOT_RECEIVED("I didn't receive the goods or service"),
    DUPLICATE("I was charged twice"),
}

/** Dispute. */
data class Dispute(
    val reference: String,
    val transactionId: String,
    val title: String,
    val amountMinor: Long,
    val currency: String,
    val reason: DisputeReason,
    val details: String,
    val filedAtEpochMs: Long,
)

const val REVIEW_AFTER_MS = 60_000L

/** Stage. */
fun Dispute.stage(now: Long): Int = if (now - filedAtEpochMs >= REVIEW_AFTER_MS) 1 else 0

/** Disputes filed on this phone, kept on disk until a disputes endpoint exists. A null file is memory only. */
@Singleton
class DisputeDesk internal constructor(private val file: File?) {
    @Inject constructor(@ApplicationContext context: Context) : this(File(context.filesDir, "disputes.txt"))

    private val rows = MutableStateFlow(read())
    val disputes: StateFlow<List<Dispute>> = rows.asStateFlow()

    /** File. */
    fun file(tx: Transaction, reason: DisputeReason, details: String, now: Long = System.currentTimeMillis()): Dispute {
        rows.value.firstOrNull { it.transactionId == tx.id }?.let { return it }
        val dispute = Dispute(
            reference = "DSP-${(now % 1_000_000).toString().padStart(6, '0')}",
            transactionId = tx.id,
            title = tx.description,
            amountMinor = tx.amountInCents,
            currency = tx.currency,
            reason = reason,
            details = details.trim(),
            filedAtEpochMs = now,
        )
        val next = rows.value + dispute
        rows.value = next
        write(next)
        return dispute
    }

    /** Read. */
    private fun read(): List<Dispute> = file?.takeIf { it.isFile }?.readLines()?.mapNotNull { line ->
        runCatching { decodeDispute(line) }.getOrNull()
    }.orEmpty()

    /** Write. */
    private fun write(next: List<Dispute>) {
        val target = file ?: return
        target.writeText(next.joinToString("\n") { it.encode() })
    }
}

/** Encode. */
private fun Dispute.encode(): String = Base64.getEncoder().encodeToString(
    listOf(reference, transactionId, title, amountMinor.toString(), currency, reason.name, details, filedAtEpochMs.toString())
        .joinToString("\u001F") { URLEncoder.encode(it, "UTF-8") }
        .toByteArray(),
)

/** Decode dispute. */
private fun decodeDispute(line: String): Dispute {
    val parts = String(Base64.getDecoder().decode(line)).split("\u001F").map { URLDecoder.decode(it, "UTF-8") }
    return Dispute(
        parts[0],
        parts[1],
        parts[2],
        parts[3].toLong(),
        parts[4],
        DisputeReason.valueOf(parts[5]),
        parts[6],
        parts[7].toLong(),
    )
}

/** Field diff. */
data class FieldDiff(val field: String, val mine: String, val theirs: String)

/** Conflict fields. */
fun conflictFields(local: Transaction, server: Transaction): List<FieldDiff> = listOf(
    Triple("Description", local.description, server.description),
    Triple("Category", local.category, server.category),
    Triple("Amount (minor units)", local.amountInCents.toString(), server.amountInCents.toString()),
).mapNotNull { (field, mine, theirs) ->
    (mergeField(mine, theirs) as? FieldMerge.Conflict)?.let { FieldDiff(field, it.left, it.right) }
}

/** Builds the merged record from per-field choices; `true` keeps this device's value. */
fun resolveConflict(local: Transaction, server: Transaction, keepMine: Map<String, Boolean>): Transaction = server.copy(
    description = if (keepMine["Description"] == true) local.description else server.description,
    category = if (keepMine["Category"] == true) local.category else server.category,
    amountInCents = if (keepMine["Amount (minor units)"] == true) local.amountInCents else server.amountInCents,
)

/** Upi target. */
data class UpiTarget(val vpa: String, val name: String?, val amountMinor: Long)

/** Parses upi. */
fun parseUpi(raw: String): UpiTarget? {
    val text = raw.trim()
    if (!text.startsWith("upi://pay?", ignoreCase = true)) return null
    val params = text.substringAfter('?').split('&').mapNotNull { pair ->
        val key = pair.substringBefore('=', "")
        if (key.isEmpty()) null else key.lowercase() to URLDecoder.decode(pair.substringAfter("="), "UTF-8")
    }.toMap()
    val vpa = params["pa"]?.takeIf { it.contains('@') } ?: return null
    val amount = params["am"]?.let { com.ledger.personal.ui.feature.parseMinor(it) } ?: 0L
    return UpiTarget(vpa, params["pn"], amount)
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
