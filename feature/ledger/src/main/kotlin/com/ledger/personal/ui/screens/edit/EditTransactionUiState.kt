package com.ledger.personal.ui.screens.edit

/**
 * Single immutable UI state for the Edit Transaction screen.
 */
data class EditTransactionUiState(
    val isLoadingInitial: Boolean = true,
    val transactionId: String = "",
    val amountInput: String = "",
    val initialAmountInput: String = "",
    val currency: String = "USD",
    val category: String = "",
    val initialCategory: String = "",
    val description: String = "",
    val initialDescription: String = "",
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() = !isLoadingInitial && errorMessage == null && transactionId.isBlank()
}
