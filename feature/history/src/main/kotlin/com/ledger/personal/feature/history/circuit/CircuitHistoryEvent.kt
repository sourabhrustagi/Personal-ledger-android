package com.ledger.personal.feature.history.circuit

import com.ledger.personal.feature.history.model.HistoryTransactionItem

/** Slack Circuit UI events for the History screen. */
sealed interface CircuitHistoryEvent {
    data class ChangeFilter(val category: String?) : CircuitHistoryEvent
    data class SelectTransaction(val transaction: HistoryTransactionItem?) : CircuitHistoryEvent
    data object DismissDetail : CircuitHistoryEvent
    data object Refresh : CircuitHistoryEvent
}
