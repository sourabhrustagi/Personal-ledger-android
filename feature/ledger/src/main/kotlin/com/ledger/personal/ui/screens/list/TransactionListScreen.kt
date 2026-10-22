package com.ledger.personal.ui.screens.list

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import com.ledger.personal.util.toFormattedCurrency
import com.ledger.personal.util.toFormattedDateTime

/** Shows the transaction list screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    uiState: TransactionListUiState,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onTransactionClick: (String) -> Unit,
    onCreateClick: () -> Unit,
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
                            text = AppStrings.APP_NAME,
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = AppStrings.TITLE_NEW_TRANSACTION
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
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
                    LoadingState(message = AppStrings.LOADING_TRANSACTIONS)
                }
                failure != null && (paging?.itemCount ?: uiState.items.size) == 0 -> {
                    ErrorState(
                        errorMessage = failure,
                        onRetry = onRefresh
                    )
                }
                empty -> {
                    EmptyState(
                        title = AppStrings.EMPTY_TRANSACTIONS_DEFAULT_TITLE,
                        message = AppStrings.EMPTY_TRANSACTIONS_DEFAULT_SUBTITLE,
                        actionLabel = AppStrings.ACTION_ADD_TRANSACTION,
                        onActionClick = onCreateClick
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val listError = uiState.errorMessage
                        if (listError != null) {
                            item(key = "list-error") {
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
                            items(count = paging.itemCount) { index ->
                                val transaction = paging[index] ?: return@items
                                TransactionListItem(
                                    transaction = transaction,
                                    onClick = { onTransactionClick(transaction.id) },
                                )
                            }
                        } else {
                            items(
                                items = uiState.items,
                                key = { it.id },
                            ) { transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    onClick = { onTransactionClick(transaction.id) },
                                )
                            }
                        }

                        if (uiState.isLoadingNextPage) {
                            item {
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
                    text = transaction.timestampEpochMs.toFormattedDateTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = transaction.amountInCents.toFormattedCurrency(transaction.currency),
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
        TransactionListItem(transaction = previewTransaction(), onClick = {})
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
            onTransactionClick = {},
            onCreateClick = {}
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
            onTransactionClick = {},
            onCreateClick = {}
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
            onTransactionClick = {},
            onCreateClick = {}
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
            onTransactionClick = {},
            onCreateClick = {}
        )
    }
}
