package com.ledger.personal.feature.ledger.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.ErrorState
import com.ledger.personal.ui.components.LoadingState
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.feature.ledger.util.toFormattedCurrency
import com.ledger.personal.feature.ledger.util.toFormattedDateTime

/** Shows the transaction list screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    uiState: TransactionListUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onTransactionClick: ((String) -> Unit)? = null,
    onProductsClick: () -> Unit = {},
    paging: LazyPagingItems<Transaction>? = null,
) {
    val listState = rememberLazyListState()

    // Detect when user scrolls near the bottom to trigger next page
    val shouldLoadMore by remember {
        derivedStateOf {
            val totalItems = listState.layoutInfo.totalItemsCount
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems > 0 && lastVisibleIndex >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !uiState.isLoadingNextPage && !uiState.isEndReached) {
            onLoadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val syncStatus = uiState.syncStatus
                    Column {
                        Text(
                            text = stringResource(AppStrings.APP_NAME),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        if (syncStatus != null) {
                            Text(
                                text = syncStatus,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    IconButton(onClick = onProductsClick) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "More products",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh list",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val refresh = paging?.loadState?.refresh
            val loading = if (paging != null) {
                refresh is LoadState.Loading && paging.itemCount == 0
            } else {
                uiState.isLoading && uiState.items.isEmpty() && uiState.errorMessage == null
            }
            val failure = (refresh as? LoadState.Error)?.error?.message ?: uiState.errorMessage
            val empty = if (paging != null) {
                refresh is LoadState.NotLoading && paging.itemCount == 0 && failure == null
            } else {
                uiState.isEmpty
            }
            when {
                loading -> {
                    LoadingState(message = stringResource(AppStrings.LOADING_TRANSACTIONS))
                }
                failure != null && (paging?.itemCount ?: uiState.items.size) == 0 -> {
                    ErrorState(
                        errorMessage = failure,
                        onRetry = onRefresh
                    )
                }
                empty -> {
                    EmptyState(
                        title = stringResource(AppStrings.EMPTY_TRANSACTIONS_DEFAULT_TITLE),
                        message = stringResource(AppStrings.EMPTY_TRANSACTIONS_DEFAULT_SUBTITLE)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 840.dp)
                    ) {
                        val listError = uiState.errorMessage
                        if (listError != null) {
                            item(key = "list-error", contentType = "error_banner") {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = listError,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        if (paging != null) {
                            items(
                                count = paging.itemCount,
                                key = paging.itemKey { it.id },
                                contentType = paging.itemContentType { "transaction_item" },
                            ) { index ->
                                val transaction = paging[index] ?: return@items
                                TransactionListItem(
                                    transaction = transaction,
                                    onClick = onTransactionClick?.let { { it(transaction.id) } },
                                )
                            }
                        } else {
                            items(
                                items = uiState.items,
                                key = { it.id },
                                contentType = { "transaction_item" },
                            ) { transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    onClick = onTransactionClick?.let { { it(transaction.id) } },
                                )
                            }
                        }

                        if (uiState.isLoadingNextPage) {
                            item(key = "loading_more", contentType = "loading_spinner") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Shows transaction list item. */
@Composable
fun TransactionListItem(
    transaction: Transaction,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val formattedDateTime = remember(transaction.timestampEpochMs) {
        transaction.timestampEpochMs.toFormattedDateTime()
    }
    val formattedCurrency = remember(transaction.amountInCents, transaction.currency) {
        transaction.amountInCents.toFormattedCurrency(transaction.currency)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedDateTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = formattedCurrency,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Preview transaction. */
private fun previewTransaction() = Transaction(
    id = "tx-preview-1",
    amountInCents = 4550L,
    currency = "USD",
    category = "Dining",
    description = "Team lunch",
    timestampEpochMs = 1_715_000_000_000L
)

/** Shows transaction list item preview. */
@PreviewLightDark
@Composable
private fun TransactionListItemPreview() {
    PersonalLedgerTheme {
        TransactionListItem(transaction = previewTransaction())
    }
}

/** Shows transaction list loading preview. */
@PreviewLightDark
@Composable
private fun TransactionListLoadingPreview() {
    PersonalLedgerTheme {
        TransactionListScreen(
            uiState = TransactionListUiState(isLoading = true),
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

/** Shows transaction list empty preview. */
@PreviewLightDark
@Composable
private fun TransactionListEmptyPreview() {
    PersonalLedgerTheme {
        TransactionListScreen(
            uiState = TransactionListUiState(isLoading = false),
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

/** Shows transaction list error preview. */
@PreviewLightDark
@Composable
private fun TransactionListErrorPreview() {
    PersonalLedgerTheme {
        TransactionListScreen(
            uiState = TransactionListUiState(errorMessage = "Failed to load transactions."),
            onRefresh = {},
            onLoadMore = {},
        )
    }
}

/** Shows transaction list populated preview. */
@PreviewLightDark
@Composable
private fun TransactionListPopulatedPreview() {
    PersonalLedgerTheme {
        TransactionListScreen(
            uiState = TransactionListUiState(
                items = listOf(
                    previewTransaction(),
                    previewTransaction().copy(
                        id = "tx-preview-2",
                        category = "Utilities",
                        description = "Electricity bill",
                        amountInCents = 12800L
                    )
                )
            ),
            onRefresh = {},
            onLoadMore = {},
        )
    }
}
