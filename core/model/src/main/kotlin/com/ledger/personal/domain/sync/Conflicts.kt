package com.ledger.personal.domain.sync

import com.ledger.personal.domain.model.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

/** The server rejected a push because it already has a different copy. */
class VersionConflict(val server: Transaction) : Exception("Server has a newer copy of ${server.id}")

data class SyncConflict(val local: Transaction, val server: Transaction)

/**
 * Conflicts waiting for the user. Stored on disk so they survive a restart.
 * A null file keeps the list in memory, which is what tests use.
 */
class ConflictInbox(private val file: File?) {
    private val rows = MutableStateFlow(read())
    val conflicts: StateFlow<List<SyncConflict>> = rows.asStateFlow()

    fun add(conflict: SyncConflict) = replace(rows.value.filterNot { it.local.id == conflict.local.id } + conflict)

    fun remove(id: String) = replace(rows.value.filterNot { it.local.id == id })

    private fun replace(next: List<SyncConflict>) {
        rows.value = next
        val target = file ?: return
        val tmp = File(target.parentFile, "${target.name}.tmp")
        tmp.writeText(next.joinToString("\n") { it.pack() })
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun read(): List<SyncConflict> = file?.takeIf { it.isFile }?.readLines()?.mapNotNull { line ->
        runCatching { unpack(line) }.getOrNull()
    }.orEmpty()
}

private fun SyncConflict.pack(): String = Base64.getEncoder().encodeToString("${local.pack()}\u001E${server.pack()}".toByteArray())

private fun unpack(line: String): SyncConflict {
    val (left, right) = String(Base64.getDecoder().decode(line)).split("\u001E", limit = 2)
    return SyncConflict(left.toTransaction(), right.toTransaction())
}

private fun Transaction.pack(): String =
    listOf(id, amountInCents.toString(), currency, category, description, timestampEpochMs.toString())
        .joinToString("\u001F") { URLEncoder.encode(it, "UTF-8") }

private fun String.toTransaction(): Transaction {
    val parts = split("\u001F").map { URLDecoder.decode(it, "UTF-8") }
    return Transaction(parts[0], parts[1].toLong(), parts[2], parts[3], parts[4], parts[5].toLong())
}
