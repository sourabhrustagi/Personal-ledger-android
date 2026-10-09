package com.ledger.personal.testing

import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.settings.UserSettings
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.SyncPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState

fun sampleDto(
    id: String = "tx-1",
    amountInCents: Long = 2550L,
    currency: String = "USD",
    category: String = "Dining",
    description: String = "Lunch",
    timestampEpochMs: Long = 1_715_000_000_000L
) = TransactionDto(
    id = id,
    amountInCents = amountInCents,
    currency = currency,
    category = category,
    description = description,
    timestampEpochMs = timestampEpochMs
)

class ScriptedTransactionRepository : TransactionRepository {
    var getTransactionsResult: Result<List<Transaction>> = Result.success(emptyList())
    var getByIdResult: Result<Transaction> = Result.failure(NoSuchElementException("missing"))
    var createResult: Result<Transaction> = Result.failure(IllegalStateException("create failed"))
    var patchResult: Result<Transaction> = Result.failure(IllegalStateException("patch failed"))
    var deleteResult: Result<Unit> = Result.failure(IllegalStateException("delete failed"))

    var lastCreateAmount: Long? = null
    var lastCreateDescription: String? = null
    var lastPatchId: String? = null
    var lastPatchCategory: String? = null
    var lastPatchDescription: String? = null
    var lastPatchAmount: Long? = null
    var getTransactionsCalls = 0

    override suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>> {
        getTransactionsCalls++
        return getTransactionsResult
    }

    override suspend fun getTransactionById(id: String): Result<Transaction> = getByIdResult

    override suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String
    ): Result<Transaction> {
        lastCreateAmount = amountInCents
        lastCreateDescription = description
        return createResult
    }

    override suspend fun patchTransaction(
        id: String,
        category: String?,
        description: String?,
        amountInCents: Long?
    ): Result<Transaction> {
        lastPatchId = id
        lastPatchCategory = category
        lastPatchDescription = description
        lastPatchAmount = amountInCents
        return patchResult
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> = deleteResult

    override fun observeTransactions(): Flow<List<Transaction>> = flow {
        emit(getTransactionsResult.getOrElse { throw it })
    }

    override fun pages(): Flow<PagingData<Transaction>> = Pager(PagingConfig(pageSize = 20)) {
        object : PagingSource<Int, Transaction>() {
            override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> =
                getTransactionsResult.fold(
                    onSuccess = { LoadResult.Page(it, null, null) },
                    onFailure = { LoadResult.Error(it) },
                )

            override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? = null
        }
    }.flow

    override fun observeTransaction(id: String): Flow<Transaction?> = flow {
        getByIdResult.fold(
            onSuccess = { emit(it) },
            onFailure = { throw it },
        )
    }
}

class FakeSyncCoordinator : SyncCoordinator {
    var enqueued = 0
    private val phase = MutableStateFlow(SyncPhase.IDLE)

    override fun enqueue() {
        enqueued += 1
    }

    override fun observe(): Flow<SyncPhase> = phase
}

class FakeUserSettings : UserSettings {
    override val lastSyncEpochMs: Flow<Long?> = MutableStateFlow(null)
    override suspend fun recordSync(epochMs: Long) = Unit
}
