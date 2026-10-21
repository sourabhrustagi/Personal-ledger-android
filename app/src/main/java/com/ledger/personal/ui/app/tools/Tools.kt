package com.ledger.personal.ui.app.tools

import androidx.paging.PagingData
import com.ledger.personal.data.tools.DisputeDesk
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.ConflictInbox
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal fun previewTools() = ToolsViewModel(PreviewTransactions, DisputeDesk(null), ConflictInbox(null))

internal object PreviewTransactions : TransactionRepository {
    private val rows = listOf(Transaction("tx-1", 50_000L, "INR", "Utilities", "Electricity", 1_760_200_000_000L))

    override fun observeTransactions() = flowOf(rows)
    override fun observeTransaction(id: String) = flowOf(rows.find { it.id == id })
    override fun pages(): Flow<PagingData<Transaction>> = flowOf(PagingData.from(rows))
    override suspend fun getTransactions(page: Int, pageSize: Int) = Result.success(rows)
    override suspend fun getTransactionById(id: String) =
        rows.find { it.id == id }?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(id))
    override suspend fun createTransaction(amountInCents: Long, currency: String, category: String, description: String) =
        Result.failure<Transaction>(UnsupportedOperationException())
    override suspend fun patchTransaction(id: String, category: String?, description: String?, amountInCents: Long?) =
        Result.failure<Transaction>(UnsupportedOperationException())
    override suspend fun deleteTransaction(id: String) = Result.success(Unit)
}
