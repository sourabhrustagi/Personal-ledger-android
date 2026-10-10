package com.ledger.personal.domain.repository

import com.ledger.personal.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

/** Contract for transaction remote. */
interface TransactionRemote {
    /** Returns transactions. */
    suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>>
    /** Returns transaction by id. */
    suspend fun getTransactionById(id: String): Result<Transaction>
    /** Creates transaction. */
    suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String,
    ): Result<Transaction>

    /** Patch transaction. */
    suspend fun patchTransaction(
        id: String,
        category: String? = null,
        description: String? = null,
        amountInCents: Long? = null,
    ): Result<Transaction>

    /** Deletes transaction. */
    suspend fun deleteTransaction(id: String): Result<Unit>
}

/** Reads and writes transaction. */
interface TransactionRepository : TransactionRemote {
    /** Streams transactions. */
    fun observeTransactions(): Flow<List<Transaction>>
    /** Streams transaction. */
    fun observeTransaction(id: String): Flow<Transaction?>
    /** Pages. */
    fun pages(): Flow<PagingData<Transaction>>
}

