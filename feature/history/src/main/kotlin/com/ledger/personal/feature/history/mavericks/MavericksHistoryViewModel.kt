package com.ledger.personal.feature.history.mavericks

import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.feature.history.model.toItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

/**
 * ViewModel orchestrating History using Airbnb's Mavericks (MvRx) architecture.
 */
class MavericksHistoryViewModel(
    private val repository: TransactionRepository,
    scope: CoroutineScope,
) : MavericksViewModel<MavericksHistoryState>(
    initialState = MavericksHistoryState(),
    viewModelScope = scope,
) {

    init {
        loadTransactions()
    }

    fun loadTransactions() {
        repository.observeTransactions()
            .map { list -> list.map { it.toItem() } }
            .execute { asyncList -> copy(transactions = asyncList) }
    }

    fun selectCategory(category: String?) {
        setState { copy(selectedCategory = category) }
    }

    fun selectTransaction(transaction: HistoryTransactionItem?) {
        setState { copy(selectedTransaction = transaction) }
    }

    fun dismissDetail() {
        setState { copy(selectedTransaction = null) }
    }

    fun refresh() {
        suspend {
            repository.refresh()
            val list = repository.getTransactions(1, 50).getOrThrow()
            list.map { it.toItem() }
        }.execute { copy(transactions = it) }
    }
}
