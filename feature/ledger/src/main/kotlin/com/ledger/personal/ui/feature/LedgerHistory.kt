package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.ledger.personal.data.local.TransactionDao
import com.ledger.personal.data.local.TransactionEntity
import com.ledger.personal.data.local.toEntity
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.flow.flowOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState
import com.ledger.personal.data.local.toDomain
import com.ledger.personal.util.toFormattedCurrency
import com.ledger.personal.util.toFormattedDateTime

/** Shows the ledger history screen. */
@Composable
fun LedgerHistoryScreen(onBack: () -> Unit, viewModel: LedgerHistoryViewModel = hiltViewModel()) {
    val pages = viewModel.pages.collectAsLazyPagingItems()
    FeatureScaffold("History", onBack, subtitle = "Everything saved on this device", scrollable = false) {
        val loading = pages.loadState.refresh is LoadState.Loading
        LoadingOrEmpty(loading && pages.itemCount == 0, isEmpty = false, errorMessage = null, label = "transactions")
        if (pages.itemCount == 0 && !loading) {
            EmptyState(Icons.AutoMirrored.Outlined.ReceiptLong, "Add your first transaction", "Transactions you record show up here.")
        }
        SectionCard(Modifier.weight(1f, fill = false)) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(pages.itemCount) { index ->
                    val row = pages[index]?.toDomain() ?: return@items
                    ListRow(
                        icon = categoryIcon(row.category),
                        title = row.description.ifBlank { row.category },
                        subtitle = "${row.category} · ${row.timestampEpochMs.toFormattedDateTime()}",
                        trailing = row.amountInCents.toFormattedCurrency(row.currency),
                        trailingColor = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
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
