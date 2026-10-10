package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.LoadState
import com.ledger.personal.data.local.TransactionDao
import com.ledger.personal.data.local.TransactionEntity
import com.ledger.personal.data.local.toDomain
import com.ledger.personal.util.toFormattedCurrency
import com.ledger.personal.util.toFormattedDateTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** State and actions for the ledger history screen. */
@HiltViewModel
class LedgerHistoryViewModel @Inject constructor(dao: TransactionDao) : ViewModel() {
    val pages: Flow<PagingData<TransactionEntity>> =
        Pager(PagingConfig(pageSize = 20)) { dao.pagingSource() }.flow.cachedIn(viewModelScope)
}

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
