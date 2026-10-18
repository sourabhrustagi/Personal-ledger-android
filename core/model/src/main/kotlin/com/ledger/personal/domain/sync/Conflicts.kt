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
class VersionConflict(val server: Transaction, cause: Throwable? = null) :
    Exception("Server has a newer copy of ${server.id}", cause)

/** Sync conflict. */
data class SyncConflict(val local: Transaction, val server: Transaction)

/**
 * Conflicts waiting for the user. Stored on disk so they survive a restart.
 * A null file keeps the list in memory, which is what tests use.
 */
class ConflictInbox(private val file: File?) {
    private val rows = MutableStateFlow(read())
    val conflicts: StateFlow<List<SyncConflict>> = rows.asStateFlow()

    /** Add. */
    fun add(conflict: SyncConflict) = replace(rows.value.filterNot { it.local.id == conflict.local.id } + conflict)

    /** Remove. */
    fun remove(id: String) = replace(rows.value.filterNot { it.local.id == id })

    /** Replace. */
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

    /** Read. */
    private fun read(): List<SyncConflict> = file?.takeIf { it.isFile }?.readLines()?.mapNotNull { line ->
        runCatching { unpack(line) }.getOrNull()
    }.orEmpty()
}

/** Pack. */
private fun SyncConflict.pack(): String = Base64.getEncoder().encodeToString("${local.pack()}\u001E${server.pack()}".toByteArray())

/** Unpack. */
private fun unpack(line: String): SyncConflict {
    val (left, right) = String(Base64.getDecoder().decode(line)).split("\u001E", limit = 2)
    return SyncConflict(left.toTransaction(), right.toTransaction())
}

/** Pack. */
private fun Transaction.pack(): String =
    listOf(id, amountInCents.toString(), currency, category, description, timestampEpochMs.toString())
        .joinToString("\u001F") { URLEncoder.encode(it, "UTF-8") }

/** Converts this value to transaction. */
private fun String.toTransaction(): Transaction {
    val parts = split("\u001F").map { URLDecoder.decode(it, "UTF-8") }
    return Transaction(
        requireNotNull(parts.getOrNull(0)) { "missing id" },
        requireNotNull(parts.getOrNull(1)?.toLongOrNull()) { "amount is not a number" },
        requireNotNull(parts.getOrNull(2)) { "missing currency" },
        requireNotNull(parts.getOrNull(3)) { "missing category" },
        requireNotNull(parts.getOrNull(4)) { "missing description" },
        requireNotNull(parts.getOrNull(5)?.toLongOrNull()) { "timestamp is not a number" },
    )
}
