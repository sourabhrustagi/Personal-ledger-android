package com.ledger.personal.domain.usecase

import com.ledger.personal.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Use case to delete a transaction by ID.
 */
class DeleteTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(id: String): Result<Unit> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("Transaction id cannot be blank"))
        }
        return repository.deleteTransaction(id)
    }
}
