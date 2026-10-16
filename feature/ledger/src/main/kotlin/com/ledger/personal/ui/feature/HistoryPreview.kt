package com.ledger.personal.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ledger.personal.data.local.TransactionDao
import com.ledger.personal.data.local.TransactionEntity
import com.ledger.personal.data.local.toEntity
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.flow.flowOf

@PreviewLightDark
@Composable
private fun LedgerHistoryPreview() {
    PersonalLedgerTheme { LedgerHistoryScreen(onBack = {}, viewModel = LedgerHistoryViewModel(PreviewDao)) }
}

private val previewRow = Transaction("tx-1", 50_000L, "INR", "Utilities", "Electricity", 1_760_200_000_000L)
    .toEntity(syncState = "SYNCED", updatedAtEpochMs = 1_760_200_000_000L)

private object PreviewDao : TransactionDao {
    override fun observeAll() = flowOf(listOf(previewRow))
    override fun pagingSource(): PagingSource<Int, TransactionEntity> = object : PagingSource<Int, TransactionEntity>() {
        override fun getRefreshKey(state: PagingState<Int, TransactionEntity>): Int? = null
        override suspend fun load(params: LoadParams<Int>): PagingSource.LoadResult<Int, TransactionEntity> =
            PagingSource.LoadResult.Page(listOf(previewRow), prevKey = null, nextKey = null)
    }
    override fun observeById(id: String) = flowOf(previewRow.takeIf { it.id == id })
    override suspend fun find(id: String) = previewRow.takeIf { it.id == id }
    override suspend fun upsert(entity: TransactionEntity) = Unit
    override suspend fun allVisible() = listOf(previewRow)
    override suspend fun pending() = emptyList<TransactionEntity>()
    override suspend fun delete(id: String) = Unit
    override suspend fun page(limit: Int, offset: Int) = if (offset == 0) listOf(previewRow) else emptyList()
}
