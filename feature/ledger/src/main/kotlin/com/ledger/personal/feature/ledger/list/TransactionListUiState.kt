package com.ledger.personal.feature.ledger.list

import com.ledger.personal.domain.model.Transaction

/**
 * Single immutable UI state representing the paginated transaction list screen.
 */
data class TransactionListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<Transaction> = emptyList(),
    val currentPage: Int = 1,
    val isLoadingNextPage: Boolean = false,
    val isEndReached: Boolean = false,
    val errorMessage: String? = null,
    val syncStatus: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && items.isEmpty()
}
