package com.ledger.personal.feature.history.decompose

import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.feature.history.model.toItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/** Decompose Component contract for transaction history. */
interface DecomposeHistoryComponent {
    val state: Value<DecomposeHistoryState>
    fun onCategorySelected(category: String?)
    fun onTransactionClicked(transaction: HistoryTransactionItem?)
    fun onDismissDetail()
    fun onRefresh()
}

/** Default implementation of [DecomposeHistoryComponent]. */
class DefaultDecomposeHistoryComponent(
    val componentContext: ComponentContext,
    private val repository: TransactionRepository,
    private val coroutineScope: CoroutineScope,
) : DecomposeHistoryComponent {

    private val _state = MutableValue(DecomposeHistoryState(isLoading = true))
    override val state: Value<DecomposeHistoryState> = _state

    init {
        observeTransactions()
    }

    private fun observeTransactions() {
        coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            repository.observeTransactions()
                .catch { cause ->
                    if (cause is CancellationException) throw cause
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = cause.message ?: "Failed to load transactions",
                    )
                }
                .collect { items ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        transactions = items.map { it.toItem() },
                        errorMessage = null,
                    )
                }
        }
    }

    override fun onCategorySelected(category: String?) {
        _state.value = _state.value.copy(selectedCategory = category)
    }

    override fun onTransactionClicked(transaction: HistoryTransactionItem?) {
        _state.value = _state.value.copy(selectedTransaction = transaction)
    }

    override fun onDismissDetail() {
        _state.value = _state.value.copy(selectedTransaction = null)
    }

    override fun onRefresh() {
        coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                repository.refresh()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(errorMessage = e.message ?: "Refresh failed")
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

