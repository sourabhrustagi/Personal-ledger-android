package com.ledger.personal.feature.history.mvikotlin

import androidx.compose.runtime.Immutable
import com.ledger.personal.feature.history.model.HistoryTransactionItem

/** Immutable UI state for Arkadii Ivanov's MVIKotlin History store. */
@Immutable
data class MviKotlinHistoryState(
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
