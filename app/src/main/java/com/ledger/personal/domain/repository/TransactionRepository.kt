package com.ledger.personal.domain.repository

import com.ledger.personal.domain.model.Transaction

interface TransactionRepository {
    suspend fun getTransactions(page: Int, pageSize: Int): Result<List<Transaction>>
    suspend fun getTransactionById(id: String): Result<Transaction>
    suspend fun createTransaction(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String
    ): Result<Transaction>

    suspend fun patchTransaction(
        id: String,
        category: String? = null,
        description: String? = null,
        amountInCents: Long? = null
    ): Result<Transaction>

    suspend fun deleteTransaction(id: String): Result<Unit>
}
