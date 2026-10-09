package com.ledger.personal.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val repository: TransactionRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val transactionId: String? = savedStateHandle["transactionId"]
    private val reload = MutableStateFlow(0)
    private val controls = MutableStateFlow(DetailControls())

    val uiState: StateFlow<TransactionDetailUiState> = combine(observeTransaction(), controls) { loaded, controls ->
        loaded.copy(
            showDeleteConfirmation = controls.showDialog,
            isDeleting = controls.deleting,
            isDeleted = controls.deleted,
            errorMessage = controls.errorMessage ?: loaded.errorMessage,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TransactionDetailUiState(isLoading = transactionId != null),
    )

    fun loadTransaction(id: String = transactionId ?: "") {
        if (id.isBlank()) return
        controls.update { it.copy(errorMessage = null) }
        reload.update { it + 1 }
    }

    fun setDeleteDialogVisible(visible: Boolean) {
        controls.update { it.copy(showDialog = visible) }
    }

    fun confirmDelete() {
        val id = uiState.value.transaction?.id ?: return
        viewModelScope.launch {
            controls.update { it.copy(deleting = true, errorMessage = null) }
            repository.deleteTransaction(id).fold(
                onSuccess = {
                    controls.update { it.copy(deleting = false, showDialog = false, deleted = true) }
                },
                onFailure = { error ->
                    controls.update {
                        it.copy(
                            deleting = false,
                            showDialog = false,
                            errorMessage = error.message ?: "Failed to delete transaction",
                        )
                    }
                },
            )
        }
    }

    private fun observeTransaction() = reload.flatMapLatest {
        val id = transactionId
        if (id.isNullOrBlank()) {
            flowOf(TransactionDetailUiState(isLoading = false, errorMessage = "Missing transaction ID"))
        } else {
            repository.observeTransaction(id)
                .map { tx -> TransactionDetailUiState(isLoading = false, transaction = tx) }
                .catch { error ->
                    emit(
                        TransactionDetailUiState(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to find transaction",
                        ),
                    )
                }
        }
    }

    private data class DetailControls(
        val showDialog: Boolean = false,
        val deleting: Boolean = false,
        val deleted: Boolean = false,
        val errorMessage: String? = null,
    )
}
