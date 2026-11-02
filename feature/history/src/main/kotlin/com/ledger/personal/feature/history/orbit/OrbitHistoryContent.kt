package com.ledger.personal.feature.history.orbit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ledger.personal.feature.history.common.HistoryCategoryFilterRow
import com.ledger.personal.feature.history.common.HistoryDetailDialog
import com.ledger.personal.feature.history.common.HistoryTransactionRow
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.LedgerOneText
import com.ledger.personal.ui.components.LedgerOneTextRole
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.colorScheme
import kotlinx.coroutines.flow.collectLatest

/** UI content for the Orbit MVI architecture. */
@Composable
fun OrbitHistoryContent(
    viewModel: OrbitHistoryViewModel,
    modifier: Modifier = Modifier,
    onShowToast: ((String) -> Unit)? = null,
) {
    val state by viewModel.container.stateFlow.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.container.sideEffectFlow.collectLatest { effect ->
            when (effect) {
                is OrbitHistorySideEffect.ShowToast -> onShowToast?.invoke(effect.message)
                is OrbitHistorySideEffect.TransactionClicked -> {
                    // Handled if needed
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        HistoryCategoryFilterRow(
            selectedCategory = state.selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) },
        )

        state.errorMessage?.let { errorMsg ->
            LedgerOneText(
                text = errorMsg,
                role = LedgerOneTextRole.Body,
                color = colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        if (state.isLoading && state.transactions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (state.filteredTransactions.isEmpty()) {
            EmptyState(
                Icons.AutoMirrored.Outlined.ReceiptLong,
                stringResource(AppStrings.EMPTY_TRANSACTIONS_TITLE),
                stringResource(AppStrings.EMPTY_TRANSACTIONS_SUBTITLE),
            )
        } else {
            SectionCard(modifier = Modifier.weight(1f, fill = false)) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(
                        items = state.filteredTransactions,
                        key = { it.id },
                        contentType = { "orbit_history_row" },
                    ) { item ->
                        HistoryTransactionRow(
                            item = item,
                            onClick = { viewModel.selectTransaction(it) },
                        )
                    }
                }
            }
        }
    }

    state.selectedTransaction?.let { item ->
        HistoryDetailDialog(
            item = item,
            onDismiss = { viewModel.dismissDetail() },
        )
    }
}
