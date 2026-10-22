package com.ledger.personal.domain.usecase

import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe a single transaction by ID.
 */
class GetTransactionByIdUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(id: String): Flow<Transaction?> = repository.observeTransaction(id)

    suspend fun get(id: String): Result<Transaction> = repository.getTransactionById(id)
}
