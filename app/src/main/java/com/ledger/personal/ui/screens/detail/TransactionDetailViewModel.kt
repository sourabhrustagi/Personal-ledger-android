package com.ledger.personal.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: String? = savedStateHandle["transactionId"]

    private val _uiState = MutableStateFlow(TransactionDetailUiState(isLoading = true))
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        transactionId?.let { loadTransaction(it) } ?: run {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Missing transaction ID") }
        }
    }

    fun loadTransaction(id: String = transactionId ?: "") {
        if (id.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            repository.getTransactionById(id).fold(
                onSuccess = { tx ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transaction = tx,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "Failed to find transaction"
                        )
                    }
                }
            )
        }
    }

    fun setDeleteDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showDeleteConfirmation = visible) }
    }

    fun confirmDelete() {
        val id = _uiState.value.transaction?.id ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            repository.deleteTransaction(id).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false,
                            isDeleted = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            showDeleteConfirmation = false,
                            errorMessage = error.localizedMessage ?: "Failed to delete transaction"
                        )
                    }
                }
            )
        }
    }
}
