package com.ledger.personal.feature.history.voyager

import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.feature.history.model.toItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ScreenModel managing History state according to the Voyager architecture.
 */
class VoyagerHistoryScreenModel(
    private val repository: TransactionRepository,
) : StateScreenModel<VoyagerHistoryState>(VoyagerHistoryState(isLoading = true)) {

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        screenModelScope.launch {
            updateState { it.copy(isLoading = true, errorMessage = null) }
            repository.observeTransactions()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    updateState {
                        it.copy(
                            isLoading = false,
                            errorMessage = cause.message ?: "Failed to observe transactions",
                        )
                    }
                }
                .collect { domainItems ->
                    updateState {
                        it.copy(
                            isLoading = false,
                            transactions = domainItems.map { item -> item.toItem() },
                            errorMessage = null,
                        )
                    }
                }
        }
    }

    fun selectCategory(category: String?) {
        updateState { it.copy(selectedCategory = category) }
    }

    fun selectTransaction(transaction: HistoryTransactionItem?) {
        updateState { it.copy(selectedTransaction = transaction) }
    }

    fun dismissDetail() {
        updateState { it.copy(selectedTransaction = null) }
    }

    fun refresh() {
        screenModelScope.launch {
            updateState { it.copy(isLoading = true) }
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                updateState { it.copy(errorMessage = e.message ?: "Refresh failed") }
            } finally {
                updateState { it.copy(isLoading = false) }
            }
        }
    }
}

