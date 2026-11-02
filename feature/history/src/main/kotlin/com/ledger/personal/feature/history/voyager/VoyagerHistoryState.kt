package com.ledger.personal.feature.history.voyager

import androidx.compose.runtime.Immutable
import com.ledger.personal.feature.history.model.HistoryTransactionItem

/** Immutable UI state for Voyager ScreenModel. */
@Immutable
data class VoyagerHistoryState(
    val isLoading: Boolean = false,
    val transactions: List<HistoryTransactionItem> = emptyList(),
    val selectedCategory: String? = null,
    val selectedTransaction: HistoryTransactionItem? = null,
    val errorMessage: String? = null,
) {
    val filteredTransactions: List<HistoryTransactionItem>
        get() = if (selectedCategory.isNullOrBlank() || selectedCategory.equals("All", ignoreCase = true)) {
            transactions
        } else {
            transactions.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }
}

