package com.ledger.personal.ui.app

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.data.tools.DisputeDesk
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.data.tools.DisputeReason
import com.ledger.personal.data.tools.StatementSummary
import com.ledger.personal.domain.sync.SyncConflict
import com.ledger.personal.data.tools.resolveConflict
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.ui.feature.formatMoney
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/** State and actions for the tools screen. */
@HiltViewModel
class ToolsViewModel @Inject constructor(
    private val repository: TransactionRepository,
    val disputeDesk: DisputeDesk,
    val inbox: ConflictInbox,
) : ViewModel() {
    val transactions: StateFlow<List<Transaction>> =
        repository.observeTransactions().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Files dispute. */
    fun fileDispute(tx: Transaction, reason: DisputeReason, details: String) = disputeDesk.file(tx, reason, details)

    /** Mock controls: pretend the server changed the newest transaction while this device edited it. */
    fun simulateConflict() {
        viewModelScope.launch {
            val local = repository.observeTransactions().first().firstOrNull() ?: return@launch
            val server = local.copy(description = "${local.description} (edited on web)", amountInCents = local.amountInCents + 100)
            inbox.add(SyncConflict(local, server))
        }
    }

    /** Resolve. */
    fun resolve(conflict: SyncConflict, keepMine: Map<String, Boolean>, onDone: (Result<Unit>) -> Unit) {
        val merged = resolveConflict(conflict.local, conflict.server, keepMine)
        viewModelScope.launch {
            val result = repository.patchTransaction(merged.id, merged.category, merged.description, merged.amountInCents).map { }
            if (result.isSuccess) inbox.remove(conflict.local.id)
            onDone(result)
        }
    }

    /** Shares statement. */
    fun shareStatement(context: Context, summary: StatementSummary, onError: (String) -> Unit) {
        viewModelScope.launch {
            val file = runCatching { withContext(Dispatchers.IO) { writeStatementPdf(context, summary) } }
                .getOrElse { return@launch onError(it.message ?: "Couldn't create the statement") }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(send, "Save or share statement"))
        }
    }
}

private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 40f
private const val LINE = 16f

/** Write statement pdf. */
private fun writeStatementPdf(context: Context, summary: StatementSummary): File {
    val dateFormat = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val lines = buildList {
        add("LedgerOne · Savings •• 4821")
        add("Statement for ${summary.month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
        add("Money in ${formatMoney(summary.inMinor)}    Money out ${formatMoney(summary.outMinor)}")
        add("")
        summary.rows.forEach { tx ->
            val date = Instant.ofEpochMilli(tx.timestampEpochMs).atZone(ZoneId.systemDefault()).format(dateFormat)
            val sign = if (tx.category == "Salary") "+" else "-"
            add("$date   ${tx.description.take(34).padEnd(34)}  ${tx.category.padEnd(14)} $sign${formatMoney(tx.amountInCents)}")
        }
        if (summary.rows.isEmpty()) add("No transactions this month.")
    }
    val doc = PdfDocument()
    val paint = Paint().apply {
        textSize = 10f
        typeface = android.graphics.Typeface.MONOSPACE
    }
    val perPage = ((PAGE_H - 2 * MARGIN) / LINE).toInt()
    lines.chunked(perPage).forEachIndexed { index, chunk ->
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, index + 1).create())
        chunk.forEachIndexed { row, text -> page.canvas.drawText(text, MARGIN, MARGIN + row * LINE, paint) }
        doc.finishPage(page)
    }
    val dir = File(context.cacheDir, "docs").apply { mkdirs() }
    val file = File(dir, "statement-${summary.month}.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return file
}
