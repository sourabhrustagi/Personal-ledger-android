package com.ledger.personal.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.ledger.personal.data.local.SyncState
import com.ledger.personal.data.local.TransactionDao
import com.ledger.personal.data.local.TransactionEntity
import com.ledger.personal.data.local.toDomain
import com.ledger.personal.data.local.toEntity
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.domain.sync.SyncConflict
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.VersionConflict
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Reads and writes offline transaction. */
@Singleton
class OfflineTransactionRepository @Inject constructor(
    private val dao: TransactionDao,
    private val remote: TransactionRemote,
    private val syncCoordinator: SyncCoordinator,
    private val conflicts: ConflictInbox,
) : TransactionRepository {

    /** Streams transactions. */
    override fun observeTransactions(): Flow<List<Transaction>> =
        dao.observeAll().map { it.map(TransactionEntity::toDomain) }

    /** Streams transaction. */
    override fun observeTransaction(id: String): Flow<Transaction?> =
        dao.observeById(id).map { it?.toDomain() }

    /** Returns transactions. */
    override suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>> = runCatching {
        require(page >= 1) { "Page number must be >= 1" }
        require(pageSize >= 1) { "Page size must be >= 1" }
        val from = (page - 1) * pageSize
        dao.page(pageSize, from).map(TransactionEntity::toDomain)
    }

    /** Pages. */
    override fun pages(): Flow<PagingData<Transaction>> =
        Pager(PagingConfig(pageSize = 20)) { dao.pagingSource() }
            .flow
            .map { it.map(TransactionEntity::toDomain) }

    /** Returns transaction by id. */
    override suspend fun getTransactionById(id: String): Result<Transaction> {
        val row = dao.find(id)
        return if (row == null || row.syncState == SyncState.PENDING_DELETE) {
            Result.failure(NoSuchElementException("Transaction not found with id: $id"))
        } else {
            Result.success(row.toDomain())
        }
    }

    /** Creates transaction. The row is saved first; a refused write is shown and a dropped one is left to sync. */
    override suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String,
    ): Result<Transaction> {
        val created = Transaction(
            id = UUID.randomUUID().toString(),
            amountInCents = amountInCents,
            currency = currency,
            category = category,
            description = description,
            timestampEpochMs = System.currentTimeMillis(),
        )
        dao.upsert(created.toEntity(SyncState.PENDING_CREATE, created.timestampEpochMs))
        val pushed = remote.createTransaction(amountInCents, currency, category, description)
        if (pushed.isSuccess) {
            dao.delete(created.id)
            val confirmed = pushed.getOrThrow()
            dao.upsert(confirmed.toEntity(SyncState.SYNCED, created.timestampEpochMs))
            return Result.success(confirmed)
        }
        syncCoordinator.enqueue()
        return pushed
    }

    /** Patch transaction. */
    override suspend fun patchTransaction(
        id: String,
        category: String?,
        description: String?,
        amountInCents: Long?,
    ): Result<Transaction> {
        val existing = dao.find(id) ?: return Result.failure(NoSuchElementException("Transaction not found with id: $id"))
        val updated = existing.toDomain().copy(
            category = category ?: existing.category,
            description = description ?: existing.description,
            amountInCents = amountInCents ?: existing.amount.minorUnits,
        )
        val state = if (existing.syncState == SyncState.PENDING_CREATE) {
            SyncState.PENDING_CREATE
        } else {
            SyncState.PENDING_UPDATE
        }
        dao.upsert(updated.toEntity(state, System.currentTimeMillis()))
        val pushed = remote.patchTransaction(id, category, description, amountInCents)
        if (pushed.isSuccess) {
            val confirmed = pushed.getOrThrow()
            dao.upsert(confirmed.toEntity(SyncState.SYNCED, System.currentTimeMillis()))
            return Result.success(confirmed)
        }
        syncCoordinator.enqueue()
        return pushed
    }

    /** Deletes transaction. */
    override suspend fun deleteTransaction(id: String): Result<Unit> {
        val existing = dao.find(id) ?: return Result.failure(NoSuchElementException("Transaction not found with id: $id"))
        if (existing.syncState == SyncState.PENDING_CREATE) {
            dao.delete(id)
            return Result.success(Unit)
        }
        dao.upsert(existing.copy(syncState = SyncState.PENDING_DELETE, updatedAtEpochMs = System.currentTimeMillis()))
        val deleted = remote.deleteTransaction(id)
        if (deleted.isSuccess || deleted.exceptionOrNull()?.message?.contains("404") == true) {
            dao.delete(id)
            return Result.success(Unit)
        }
        syncCoordinator.enqueue()
        return deleted
    }

    /**
     * Last-write-wins. A row that still has a local pending edit is kept; a synced row is replaced
     * by the server copy. Revisit this when the server adds a version column — the client clock in
     * updatedAtEpochMs is not a substitute for a server version.
     */
    /** Refreshes transactions by synchronizing local cache with remote network. */
    override suspend fun refresh(): Result<Unit> = sync()

    suspend fun sync(): Result<Unit> = runCatching {
        pushPending()
        pullRemote()
    }.onFailure { if (it is CancellationException) throw it }

    /** Push pending. */
    private suspend fun pushPending() {
        val blocked = conflicts.conflicts.value.map { it.local.id }.toSet()
        dao.pending().forEach { row ->
            if (row.id in blocked) return@forEach
            when (row.syncState) {
                SyncState.PENDING_CREATE -> {
                    val created = remote.createTransaction(
                        amountInCents = row.amount.minorUnits,
                        currency = row.currency,
                        category = row.category,
                        description = row.description,
                    ).accepted(row) ?: return@forEach
                    dao.delete(row.id)
                    dao.upsert(created.toEntity(SyncState.SYNCED, row.updatedAtEpochMs))
                }
                SyncState.PENDING_UPDATE -> {
                    val updated = remote.patchTransaction(
                        id = row.id,
                        category = row.category,
                        description = row.description,
                        amountInCents = row.amount.minorUnits,
                    ).accepted(row) ?: return@forEach
                    dao.upsert(updated.toEntity(SyncState.SYNCED, row.updatedAtEpochMs))
                }
                SyncState.PENDING_DELETE -> {
                    val deleted = remote.deleteTransaction(row.id)
                    if (deleted.isSuccess || deleted.exceptionOrNull()?.message?.contains("404") == true) {
                        dao.delete(row.id)
                    } else {
                        deleted.getOrThrow()
                    }
                }
            }
        }
    }

    /** Pull remote. */
    private tailrec suspend fun pullRemote(page: Int = 1) {
        val batch = remote.getTransactions(page, PAGE_SIZE).getOrThrow()
        if (batch.isEmpty()) return
        batch.forEach { incoming ->
            val local = dao.find(incoming.id)
            if (local != null && local.syncState != SyncState.SYNCED) return@forEach
            dao.upsert(
                incoming.toEntity(
                    SyncState.SYNCED,
                    local?.updatedAtEpochMs ?: incoming.timestampEpochMs,
                ),
            )
        }
        if (batch.size < PAGE_SIZE) return
        pullRemote(page + 1)
    }

    /** A 409 parks the row in the conflict inbox and leaves it pending. Anything else still throws. */
    private fun Result<Transaction>.accepted(row: TransactionEntity): Transaction? {
        val conflict = exceptionOrNull() as? VersionConflict
        if (conflict != null) {
            conflicts.add(SyncConflict(row.toDomain(), conflict.server))
            return null
        }
        return getOrThrow()
    }

    private companion object {
        const val PAGE_SIZE = 50
    }
}
