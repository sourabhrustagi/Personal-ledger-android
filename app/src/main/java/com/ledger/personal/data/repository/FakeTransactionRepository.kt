package com.ledger.personal.data.repository

import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class FakeTransactionRepository : TransactionRepository {

    private val storage = ConcurrentHashMap<String, Transaction>()

    init {
        // Seed some initial data for interactive testing
        val seedCategories = listOf("Groceries", "Dining", "Transport", "Salary", "Utilities", "Entertainment")
        val now = System.currentTimeMillis()
        for (i in 1..25) {
            val id = "tx-seed-$i"
            storage[id] = Transaction(
                id = id,
                amountInCents = 1500L * i,
                currency = "USD",
                category = seedCategories[i % seedCategories.size],
                description = "Sample transaction $i",
                timestampEpochMs = now - (i * 3600000L)
            )
        }
    }

    fun insertDirect(transaction: Transaction) {
        storage[transaction.id] = transaction
    }

    fun clear() {
        storage.clear()
    }

    override suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>> {
        require(page >= 1) { "Page number must be >= 1" }
        require(pageSize >= 1) { "Page size must be >= 1" }

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

    override suspend fun getTransactionById(id: String): Result<Transaction> {
        val tx = storage[id]
        return if (tx != null) {
            Result.success(tx)
        } else {
            Result.failure(NoSuchElementException("Transaction not found with id: $id"))
        }
    }

    override suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String
    ): Result<Transaction> {
        return runCatching {
            val newTx = Transaction(
                id = UUID.randomUUID().toString(),
                amountInCents = amountInCents,
                currency = currency,
                category = category,
                description = description,
                timestampEpochMs = System.currentTimeMillis()
            )
            storage[newTx.id] = newTx
            newTx
        }
    }

    override suspend fun patchTransaction(
        id: String,
        category: String?,
        description: String?,
        amountInCents: Long?
    ): Result<Transaction> {
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
            updated
        }
    }

    override suspend fun deleteTransaction(id: String): Result<Unit> {
        return if (storage.remove(id) != null) {
            Result.success(Unit)
        } else {
            Result.failure(NoSuchElementException("Transaction not found with id: $id"))
        }
    }
}
