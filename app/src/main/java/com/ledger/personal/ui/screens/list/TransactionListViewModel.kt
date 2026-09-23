package com.ledger.personal.ui.screens.list

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
class TransactionListViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionListUiState())
    val uiState: StateFlow<TransactionListUiState> = _uiState.asStateFlow()

    private val pageSize = 15

    init {
        loadInitialPage()
    }

    fun loadInitialPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.getTransactions(page = 1, pageSize = pageSize)
            result.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            items = items,
                            currentPage = 1,
                            isEndReached = items.size < pageSize,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.localizedMessage ?: "Failed to load transactions"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = repository.getTransactions(page = 1, pageSize = pageSize)
            result.fold(
                onSuccess = { items ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            items = items,
                            currentPage = 1,
                            isEndReached = items.size < pageSize,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = error.localizedMessage ?: "Refresh failed"
                        )
                    }
                }
            )
        }
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingNextPage || state.isEndReached) return

        viewModelScope.launch {
            val nextPage = state.currentPage + 1
            _uiState.update { it.copy(isLoadingNextPage = true) }
            val result = repository.getTransactions(page = nextPage, pageSize = pageSize)
            result.fold(
                onSuccess = { newItems ->
                    _uiState.update { current ->
                        current.copy(
                            isLoadingNextPage = false,
                            currentPage = nextPage,
                            items = current.items + newItems,
                            isEndReached = newItems.size < pageSize
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingNextPage = false,
                            errorMessage = error.localizedMessage ?: "Failed to load more transactions"
                        )
                    }
                }
            )
        }
    }
}
