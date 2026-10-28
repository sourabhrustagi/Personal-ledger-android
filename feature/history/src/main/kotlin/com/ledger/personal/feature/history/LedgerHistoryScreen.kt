package com.ledger.personal.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ledger.personal.core.toFormattedCurrency
import com.ledger.personal.core.toFormattedDateTime
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.usecase.GetPagedTransactionsUseCase
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.ListRow
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.components.categoryIcon
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Shows the passbook / ledger history screen. */
@Composable
fun LedgerHistoryScreen(
    onBack: (() -> Unit)? = null,
    title: String = stringResource(AppStrings.TITLE_PASSBOOK),
    viewModel: LedgerHistoryViewModel = hiltViewModel(),
) {
    val pages = viewModel.pages.collectAsLazyPagingItems()
    val isInitialLoading = pages.loadState.refresh is LoadState.Loading && pages.itemCount == 0
    FeatureScaffold(
        title = title,
        onBack = onBack,
        subtitle = "Everything saved on this device",
        scrollable = false,
    ) {
        if (isInitialLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (pages.itemCount == 0) {
            EmptyState(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                stringResource(AppStrings.EMPTY_TRANSACTIONS_TITLE),
                stringResource(AppStrings.EMPTY_TRANSACTIONS_SUBTITLE),
            )
        } else {
            SectionCard(Modifier.weight(1f, fill = false)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(
                        count = pages.itemCount,
                        key = pages.itemKey { it.id },
                        contentType = pages.itemContentType { "passbook_row" },
                    ) { index ->
                        val row = pages[index] ?: return@items
                        val formattedDate = remember(row.timestampEpochMs) { row.timestampEpochMs.toFormattedDateTime() }
                        val formattedAmount = remember(row.amountInCents, row.currency) {
                            row.amountInCents.toFormattedCurrency(row.currency)
                        }
                        ListRow(
                            icon = categoryIcon(row.category),
                            title = row.description.ifBlank { row.category },
                            subtitle = "${row.category} · $formattedDate",
                            trailing = formattedAmount,
                            trailingColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    if (pages.loadState.append is LoadState.Loading) {
                        item(key = "append_loading") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun LedgerHistoryPreview() {
    PersonalLedgerTheme {
        LedgerHistoryScreen(
            onBack = {},
            viewModel = LedgerHistoryViewModel(GetPagedTransactionsUseCase(PreviewTransactionsRepo)),
        )
    }
}

private val previewTransaction = Transaction("tx-1", 120_00L, "INR", "Scan & Pay", "Chai Point · QR Pay", 1_760_200_000_000L)

private object PreviewTransactionsRepo : TransactionRepository {
    override fun observeTransactions() = flowOf(listOf(previewTransaction))
    override fun observeTransaction(id: String) = flowOf(previewTransaction.takeIf { it.id == id })
    override fun pages(): Flow<PagingData<Transaction>> = flowOf(PagingData.from(listOf(previewTransaction)))
    override suspend fun getTransactions(page: Int, pageSize: Int) = Result.success(listOf(previewTransaction))
    override suspend fun getTransactionById(id: String) = Result.success(previewTransaction)
    override suspend fun createTransaction(amountInCents: Long, currency: String, category: String, description: String) =
        Result.success(previewTransaction)
    override suspend fun patchTransaction(id: String, category: String?, description: String?, amountInCents: Long?) =
        Result.success(previewTransaction)
    override suspend fun deleteTransaction(id: String) = Result.success(Unit)
}
