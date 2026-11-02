package com.ledger.personal.feature.history.circuit

import androidx.compose.runtime.Immutable
import com.ledger.personal.feature.history.model.HistoryTransactionItem

/**
 * UI State for Slack Circuit History screen.
 * Contains the [eventSink] property for delivering events directly from the UI.
 */
@Immutable
data class CircuitHistoryUiState(
    val isLoading: Boolean = false,
    val transactions: List<HistoryTransactionItem> = emptyList(),
    val selectedCategory: String? = null,
    val selectedTransaction: HistoryTransactionItem? = null,
    val errorMessage: String? = null,
    val eventSink: (CircuitHistoryEvent) -> Unit,
) : CircuitUiState {
    val filteredTransactions: List<HistoryTransactionItem>
        get() = if (selectedCategory.isNullOrBlank() || selectedCategory.equals("All", ignoreCase = true)) {
            transactions
        } else {
            transactions.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
}

