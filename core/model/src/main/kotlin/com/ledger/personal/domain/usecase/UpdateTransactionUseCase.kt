package com.ledger.personal.domain.usecase

import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Use case to patch and update an existing transaction.
 */
class UpdateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        id: String,
        category: String? = null,
        description: String? = null,
        amountInCents: Long? = null,
    ): Result<Transaction> {
        if (id.isBlank()) {
            return Result.failure(IllegalArgumentException("Transaction id cannot be blank"))
        }
        if (amountInCents != null && amountInCents <= 0L) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        return repository.patchTransaction(id, category, description, amountInCents)
    }
}
