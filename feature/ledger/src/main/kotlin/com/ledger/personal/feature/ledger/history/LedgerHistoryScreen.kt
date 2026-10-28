package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.usecase.GetPagedTransactionsUseCase
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.ListRow
import com.ledger.personal.ui.components.LoadingOrEmpty
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.components.categoryIcon
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.util.toFormattedCurrency
import com.ledger.personal.util.toFormattedDateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

import androidx.compose.runtime.remember
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey

/** Shows the passbook / ledger history screen. */
@Composable
fun LedgerHistoryScreen(
    onBack: (() -> Unit)? = null,
    title: String = stringResource(AppStrings.TITLE_PASSBOOK),
    viewModel: LedgerHistoryViewModel = hiltViewModel(),
) {
    val pages = viewModel.pages.collectAsLazyPagingItems()
    FeatureScaffold(title, onBack, subtitle = "Everything saved on this device", scrollable = false) {
        val loading = pages.loadState.refresh is LoadState.Loading
        LoadingOrEmpty(loading && pages.itemCount == 0, isEmpty = false, errorMessage = null, label = "transactions")
        if (pages.itemCount == 0 && !loading) {
            EmptyState(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                stringResource(AppStrings.EMPTY_TRANSACTIONS_TITLE),
                stringResource(AppStrings.EMPTY_TRANSACTIONS_SUBTITLE)
            )
        }
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
