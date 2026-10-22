package com.ledger.personal.domain.usecase

import com.ledger.personal.domain.repository.TransactionRepository
import javax.inject.Inject

/**
 * Use case to refresh transactions from remote data source into local storage.
 */
class RefreshTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.refresh()
}
