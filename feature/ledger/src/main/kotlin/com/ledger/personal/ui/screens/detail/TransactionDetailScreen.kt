package com.ledger.personal.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.ErrorState
import com.ledger.personal.ui.feature.DetailRow
import com.ledger.personal.ui.components.LoadingState
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.util.toFormattedCurrency
import com.ledger.personal.util.toFormattedDateTime

/** Shows the transaction detail screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    uiState: TransactionDetailUiState,
    onNavigateBack: () -> Unit,
    onNavigateEdit: ((String) -> Unit)? = null,
    onRetry: () -> Unit,
    onShowDeleteDialog: ((Boolean) -> Unit)? = null,
    onConfirmDelete: (() -> Unit)? = null,
    actions: @Composable ColumnScope.(Transaction) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(AppStrings.TITLE_TRANSACTION_DETAILS), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (uiState.transaction != null) {
                        onNavigateEdit?.let { editNav ->
                            IconButton(onClick = { editNav(uiState.transaction.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(AppStrings.TITLE_EDIT_TRANSACTION)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(message = stringResource(AppStrings.LOADING_TRANSACTION_DETAILS))
                }
                uiState.errorMessage != null && uiState.transaction == null -> {
                    ErrorState(
                        errorMessage = uiState.errorMessage,
                        onRetry = onRetry
                    )
                }
                uiState.isEmpty -> {
                    EmptyState(
                        title = stringResource(AppStrings.NO_MATCHING_TRANSACTIONS),
                        message = stringResource(AppStrings.EMPTY_TRANSACTION_NOT_FOUND),
                        actionLabel = stringResource(AppStrings.ACTION_BACK_TO_LIST),
                        onActionClick = onNavigateBack
                    )
                }
                uiState.transaction != null -> {
                    val tx = uiState.transaction
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val detailError = uiState.errorMessage
                        if (detailError != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = detailError,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = tx.category,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = tx.amountInCents.toFormattedCurrency(tx.currency),
                                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = tx.description,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                DetailRow(label = "Transaction ID", value = tx.id)
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                DetailRow(label = "Date & Time", value = tx.timestampEpochMs.toFormattedDateTime())
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                DetailRow(label = "Currency Code", value = tx.currency)
                                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                                DetailRow(label = "Minor Units", value = "${tx.amountInCents} cents")
                            }
                        }
                        actions(tx)
                    }
                }
            }
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

/** Shows transaction detail loading preview. */
@PreviewLightDark
@Composable
private fun TransactionDetailLoadingPreview() {
    PersonalLedgerTheme {
        TransactionDetailScreen(
            uiState = TransactionDetailUiState(isLoading = true),
            onNavigateBack = {},
            onNavigateEdit = {},
            onRetry = {},
        )
    }
}

/** Shows transaction detail error preview. */
@PreviewLightDark
@Composable
private fun TransactionDetailErrorPreview() {
    PersonalLedgerTheme {
        TransactionDetailScreen(
            uiState = TransactionDetailUiState(errorMessage = "Failed to find transaction"),
            onNavigateBack = {},
            onNavigateEdit = {},
            onRetry = {},
        )
    }
}

/** Shows transaction detail empty preview. */
@PreviewLightDark
@Composable
private fun TransactionDetailEmptyPreview() {
    PersonalLedgerTheme {
        TransactionDetailScreen(
            uiState = TransactionDetailUiState(),
            onNavigateBack = {},
            onNavigateEdit = {},
            onRetry = {},
        )
    }
}

/** Shows transaction detail content preview. */
@PreviewLightDark
@Composable
private fun TransactionDetailContentPreview() {
    PersonalLedgerTheme {
        TransactionDetailScreen(
            uiState = TransactionDetailUiState(transaction = previewTransaction()),
            onNavigateBack = {},
            onNavigateEdit = {},
            onRetry = {},
        )
    }
}
