package com.ledger.personal.testing

import com.ledger.personal.data.remote.dto.TransactionDto
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository

fun sampleTransaction(
    id: String = "tx-1",
    amountInCents: Long = 2550L,
    currency: String = "USD",
    category: String = "Dining",
    description: String = "Lunch",
    timestampEpochMs: Long = 1_715_000_000_000L
) = Transaction(
    id = id,
    amountInCents = amountInCents,
    currency = currency,
    category = category,
    description = description,
    timestampEpochMs = timestampEpochMs
)

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
}
