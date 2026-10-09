package com.ledger.personal.ui.screens.detail

import com.ledger.personal.domain.model.Transaction

/**
 * Single immutable UI state for the transaction detail view and delete confirmation.
 */
data class TransactionDetailUiState(
    val isLoading: Boolean = false,
    val transaction: Transaction? = null,
    val errorMessage: String? = null,
    val showDeleteConfirmation: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && transaction == null
}
