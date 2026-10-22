package com.ledger.personal.domain.usecase

import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Use case to validate and record a new transaction.
 */
class CreateTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(
        amountInCents: Long,
        currency: String,
        category: String,
        description: String,
    ): Result<Transaction> {
        if (amountInCents <= 0L) {
            return Result.failure(IllegalArgumentException("Amount must be greater than zero"))
        }
        if (category.isBlank()) {
            return Result.failure(IllegalArgumentException("Category cannot be blank"))
        }
        return repository.createTransaction(amountInCents, currency, category, description)
    }
}
