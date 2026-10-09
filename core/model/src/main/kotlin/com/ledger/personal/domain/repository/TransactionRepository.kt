package com.ledger.personal.domain.repository

import com.ledger.personal.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import androidx.paging.PagingData

interface TransactionRemote {
    suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>>
    suspend fun getTransactionById(id: String): Result<Transaction>
    suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String,
    ): Result<Transaction>

    suspend fun patchTransaction(
        id: String,
        category: String? = null,
        description: String? = null,
        amountInCents: Long? = null,
    ): Result<Transaction>

    suspend fun deleteTransaction(id: String): Result<Unit>
}

interface TransactionRepository : TransactionRemote {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: String): Flow<Transaction?>
    fun pages(): Flow<PagingData<Transaction>>
}

