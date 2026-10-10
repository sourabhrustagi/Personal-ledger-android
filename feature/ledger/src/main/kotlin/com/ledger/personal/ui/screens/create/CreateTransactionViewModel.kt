package com.ledger.personal.ui.screens.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.transactionFailureMessage
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.util.toMinorUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State and actions for the create transaction screen. */
@HiltViewModel
class CreateTransactionViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateTransactionUiState())
    val uiState: StateFlow<CreateTransactionUiState> = _uiState.asStateFlow()

    /** Reacts to amount change. */
    fun onAmountChange(input: String) {
        _uiState.update { it.copy(amountInput = input, amountError = null, errorMessage = null) }
    }

    /** Reacts to category change. */
    fun onCategoryChange(input: String) {
        _uiState.update { it.copy(category = input, categoryError = null, errorMessage = null) }
    }

    /** Reacts to description change. */
    fun onDescriptionChange(input: String) {
        _uiState.update { it.copy(description = input, errorMessage = null) }
    }

    /** Submit. */
    fun submit() {
        val state = _uiState.value
        val minorUnits = state.amountInput.toMinorUnits()
        var hasError = false

        if (minorUnits == null || minorUnits <= 0L) {
            _uiState.update { it.copy(amountError = "Please enter a valid positive amount (e.g. 12.50)") }
            hasError = true
        }

        if (state.category.isBlank()) {
            _uiState.update { it.copy(categoryError = "Category is required") }
            hasError = true
        }

        val amountInCents = minorUnits
        if (hasError || amountInCents == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            repository.createTransaction(
                amountInCents = amountInCents,
                currency = state.currency,
                category = state.category.trim(),
                description = state.description.trim().ifBlank { "Uncategorized" }
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = transactionFailureMessage(error)
                        )
                    }
                }
            )
        }
    }
}
