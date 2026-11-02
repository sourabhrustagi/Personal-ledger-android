package com.ledger.personal.feature.history.orbit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.feature.history.model.toItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel orchestrating History using the Orbit MVI architecture.
 */
@HiltViewModel
class OrbitHistoryViewModel @Inject constructor(
    private val repository: TransactionRepository,
) : ViewModel(), ContainerHost<OrbitHistoryState, OrbitHistorySideEffect> {

    override val container: Container<OrbitHistoryState, OrbitHistorySideEffect> =
        OrbitContainer(OrbitHistoryState(isLoading = true), viewModelScope)

    init {
        observeTransactions()
    }

    fun observeTransactions() {
        viewModelScope.launch {
            intent {
                reduce { it.copy(isLoading = true, errorMessage = null) }
            }
            repository.observeTransactions()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    intent {
                        reduce { it.copy(isLoading = false, errorMessage = cause.message ?: "Failed to load transactions") }
                        postSideEffect(OrbitHistorySideEffect.ShowToast("Error: ${cause.message}"))
                    }
                }
                .collect { domainList ->
                    val items = domainList.map { it.toItem() }
                    intent {
                        reduce {
                            it.copy(
                                isLoading = false,
                                transactions = items,
                                errorMessage = null,
                            )
                        }
                    }
                }
        }
    }

    fun selectCategory(category: String?) = intent {
        reduce { it.copy(selectedCategory = category) }
    }

    fun selectTransaction(item: HistoryTransactionItem?) = intent {
        reduce { it.copy(selectedTransaction = item) }
        if (item != null) {
            postSideEffect(OrbitHistorySideEffect.TransactionClicked(item.id))
        }
    }

    fun dismissDetail() = intent {
        reduce { it.copy(selectedTransaction = null) }
    }

    fun refresh() = intent {
        reduce { it.copy(isLoading = true) }
        try {
            repository.refresh()
            postSideEffect(OrbitHistorySideEffect.ShowToast("Refreshed successfully"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            reduce { it.copy(errorMessage = e.message ?: "Refresh failed") }
        } finally {
            reduce { it.copy(isLoading = false) }
        }
    }
}

