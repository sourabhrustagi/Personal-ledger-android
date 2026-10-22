package com.ledger.personal.ui.screens.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.transactionFailureMessage
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.usecase.GetTransactionByIdUseCase
import com.ledger.personal.domain.usecase.UpdateTransactionUseCase
import com.ledger.personal.util.toMajorUnitsString
import com.ledger.personal.util.toMinorUnits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State and actions for the edit transaction screen. */
@HiltViewModel
class EditTransactionViewModel @Inject constructor(
    private val getTransactionByIdUseCase: GetTransactionByIdUseCase,
    private val updateTransactionUseCase: UpdateTransactionUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    constructor(
        repository: TransactionRepository,
        savedStateHandle: SavedStateHandle,
    ) : this(
        GetTransactionByIdUseCase(repository),
        UpdateTransactionUseCase(repository),
        savedStateHandle,
    )

    private val transactionId: String? = savedStateHandle["transactionId"]

    private val _uiState = MutableStateFlow(EditTransactionUiState())
    val uiState: StateFlow<EditTransactionUiState> = _uiState.asStateFlow()

    init {
        transactionId?.let { loadInitial(it) } ?: run {
            _uiState.update {
                it.copy(isLoadingInitial = false, errorMessage = "Missing transaction ID")
            }
        }
    }

    /** Loads initial. */
    fun loadInitial(id: String = transactionId ?: "") {
        if (id.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingInitial = true, errorMessage = null) }
            getTransactionByIdUseCase.get(id).fold(
                onSuccess = { tx ->
                    val majorAmount = tx.amountInCents.toMajorUnitsString()
                    _uiState.update {
                        it.copy(
                            isLoadingInitial = false,
                            transactionId = tx.id,
                            amountInput = majorAmount,
                            initialAmountInput = majorAmount,
                            currency = tx.currency,
                            category = tx.category,
                            initialCategory = tx.category,
                            description = tx.description,
                            initialDescription = tx.description,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingInitial = false,
                            errorMessage = error.localizedMessage ?: "Failed to load transaction",
                        )
                    }
                },
            )
        }
    }

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

    /** Submits patch. */
    fun submitPatch() {
        val state = _uiState.value
        val minorUnits = state.amountInput.toMinorUnits()
        var hasError = false

        if (minorUnits == null || minorUnits <= 0L) {
            _uiState.update { it.copy(amountError = "Enter a valid positive amount") }
            hasError = true
        }

        if (state.category.isBlank()) {
            _uiState.update { it.copy(categoryError = "Category cannot be blank") }
            hasError = true
        }

        if (hasError) return

        // Compute delta for PATCH: only send fields that changed
        val patchedCategory = if (state.category != state.initialCategory) state.category.trim() else null
        val patchedDesc = if (state.description != state.initialDescription) state.description.trim() else null
        val patchedAmount = if (state.amountInput != state.initialAmountInput) minorUnits else null

        // If nothing changed, treat as success immediately
        if (patchedCategory == null && patchedDesc == null && patchedAmount == null) {
            _uiState.update { it.copy(isSuccess = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            updateTransactionUseCase(
                id = state.transactionId,
                category = patchedCategory,
                description = patchedDesc,
                amountInCents = patchedAmount,
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSubmitting = false, isSuccess = true) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = transactionFailureMessage(error),
                        )
                    }
                },
            )
        }
    }
}
