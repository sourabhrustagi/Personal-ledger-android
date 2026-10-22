package com.ledger.personal.ui.screens.create

import com.ledger.personal.core.LedgerConstants

/**
 * Single immutable UI state for the Create Transaction form screen.
 */
data class CreateTransactionUiState(
    val amountInput: String = "",
    val currency: String = LedgerConstants.CURRENCY_USD,
    val category: String = "",
    val description: String = "",
    val amountError: String? = null,
    val categoryError: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
