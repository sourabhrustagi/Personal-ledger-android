package com.ledger.personal.data.repository

import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.mock.MockSwitches
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** Reads and writes mock transaction. */
class MockTransactionRepository @Inject constructor() : TransactionRepository {

    private val storage = ConcurrentHashMap<String, Transaction>()
    private val revisions = MutableStateFlow(0)

    /** Publish. */
    private fun publish() {
        revisions.value += 1
        activePage?.invalidate()
    }

    private var activePage: PagingSource<Int, Transaction>? = null

    init {
        // Seed some initial data for interactive testing
        val seeds = listOf(
            Triple("Groceries", "BigBasket", 1_842_50L), Triple("Dining", "Swiggy", 450_00L),
            Triple("Transport", "Uber", 312_00L), Triple("Utilities", "Electricity bill", 1_290_00L),
            Triple("Entertainment", "BookMyShow", 760_00L), Triple("Groceries", "DMart", 2_315_75L),
            Triple("Dining", "Starbucks", 385_00L), Triple("Transport", "Metro card", 500_00L),
        )
        val now = System.currentTimeMillis()
        for (i in 1..25) {
            val id = "tx-seed-$i"
            val (category, merchant, amount) = if (i == 12) Triple("Salary", "Salary · Acme Corp", 85_000_00L) else seeds[i % seeds.size]
            storage[id] = Transaction(
                id = id,
                amountInCents = amount,
                currency = if (i % 9 == 0) "USD" else "INR",
                category = category,
                description = merchant,
                timestampEpochMs = now - (i * 9 * 3600000L)
            )
        }
    }

    /** Insert direct. */
    fun insertDirect(transaction: Transaction) {
        storage[transaction.id] = transaction
        publish()
    }

    /** Clear. */
    fun clear() {
        storage.clear()
        publish()
    }

    /** Streams transactions. */
    override fun observeTransactions(): Flow<List<Transaction>> = revisions.map { sortedSnapshot() }

    /** Pages. */
    override fun pages(): Flow<PagingData<Transaction>> = Pager(
        config = PagingConfig(pageSize = 20),
        pagingSourceFactory = { pageSource().also { activePage = it } },
    ).flow

    /** Page source. */
    private fun pageSource(): PagingSource<Int, Transaction> = object : PagingSource<Int, Transaction>() {
        /** Load. */
        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Transaction> {
            val all = sortedSnapshot()
            val start = params.key ?: 0
            val end = minOf(start + params.loadSize, all.size)
            return LoadResult.Page(
                data = all.subList(start, end),
                prevKey = if (start == 0) null else (start - params.loadSize).coerceAtLeast(0),
                nextKey = if (end >= all.size) null else end,
            )
        }

        /** Returns refresh key. */
        override fun getRefreshKey(state: PagingState<Int, Transaction>): Int? = state.anchorPosition
    }

    /** Streams transaction. */
    override fun observeTransaction(id: String): Flow<Transaction?> = revisions.map { storage[id] }

    /** Sorted snapshot. */
    private fun sortedSnapshot(): List<Transaction> = storage.values.sortedWith(
        compareByDescending<Transaction> { it.timestampEpochMs }.thenByDescending { it.id },
    )

    /** Returns transactions. */
    override suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>> {
        require(page >= 1) { "Page number must be >= 1" }
        require(pageSize >= 1) { "Page size must be >= 1" }
        return MockSwitches.gate { pageOf(page, pageSize) }
    }

    /** Page of. */
    private fun pageOf(page: Int, pageSize: Int): Result<List<Transaction>> {
        if (MockSwitches.empty.value) return Result.success(emptyList())

        // Stable sort: timestamp descending, tie-break by ID descending
        val sortedList = storage.values.sortedWith(
            compareByDescending<Transaction> { it.timestampEpochMs }
                .thenByDescending { it.id }
        )

        val fromIndex = (page - 1) * pageSize
        if (fromIndex >= sortedList.size) {
            return Result.success(emptyList())
        }

        val toIndex = minOf(fromIndex + pageSize, sortedList.size)
        return Result.success(sortedList.subList(fromIndex, toIndex))
    }

    /** Returns transaction by id. */
    override suspend fun getTransactionById(id: String): Result<Transaction> = MockSwitches.gate { byId(id) }

    /** By id. */
    private fun byId(id: String): Result<Transaction> {
        val tx = storage[id]
        return if (tx != null) {
            Result.success(tx)
        } else {
            Result.failure(NoSuchElementException("Transaction not found with id: $id"))
        }
    }

    /** Creates transaction. */
    override suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String
    ): Result<Transaction> = MockSwitches.gate {
        runCatching {
            val newTx = Transaction(
                id = UUID.randomUUID().toString(),
                amountInCents = amountInCents,
                currency = currency,
                category = category,
                description = description,
                timestampEpochMs = System.currentTimeMillis()
            )
            storage[newTx.id] = newTx
            publish()
            newTx
        }
    }

    /** Patch transaction. */
    override suspend fun patchTransaction(
        id: String,
        category: String?,
        description: String?,
        amountInCents: Long?
    ): Result<Transaction> = MockSwitches.gate { patch(id, category, description, amountInCents) }

    /** Patch. */
    private fun patch(id: String, category: String?, description: String?, amountInCents: Long?): Result<Transaction> {
        val existing = storage[id]
            ?: return Result.failure(NoSuchElementException("Transaction not found with id: $id"))

        return runCatching {
            val updated = existing.copy(
                category = category ?: existing.category,
                description = description ?: existing.description,
                amountInCents = amountInCents ?: existing.amountInCents
            )
            // Re-validate invariant
            require(updated.amountInCents > 0L) { "Amount must be strictly positive" }
            storage[id] = updated
            publish()
            updated
        }
    }

    /** Deletes transaction. */
    override suspend fun deleteTransaction(id: String): Result<Unit> = MockSwitches.gate {
        if (storage.remove(id) != null) {
            publish()
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Transaction not found with id: $id"))
        }
    }
}
