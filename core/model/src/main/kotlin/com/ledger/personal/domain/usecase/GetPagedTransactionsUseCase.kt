package com.ledger.personal.domain.usecase

import androidx.paging.PagingData
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to provide paged transactions stream.
 */
class GetPagedTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(): Flow<PagingData<Transaction>> = repository.pages()
}
