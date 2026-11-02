package com.ledger.personal.feature.history.mavericks

import androidx.compose.runtime.Immutable
import com.ledger.personal.feature.history.model.HistoryTransactionItem

/**
 * State object for Airbnb Mavericks History ViewModel.
 * Wraps transactions list inside [Async].
 */
@Immutable
data class MavericksHistoryState(
    val transactions: Async<List<HistoryTransactionItem>> = Async.Uninitialized,
    val selectedCategory: String? = null,
    val selectedTransaction: HistoryTransactionItem? = null,
) : MavericksState {
    val filteredTransactions: List<HistoryTransactionItem>
        get() {
            val list = transactions() ?: emptyList()
            return if (selectedCategory.isNullOrBlank() || selectedCategory.equals("All", ignoreCase = true)) {
                list
            } else {
                list.filter { it.category.equals(selectedCategory, ignoreCase = true) }
            }
        }
}
