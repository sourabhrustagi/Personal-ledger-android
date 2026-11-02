package com.ledger.personal.feature.history.mvikotlin

import com.ledger.personal.feature.history.model.HistoryTransactionItem

/** User intents dispatched to MVIKotlin store from UI. */
sealed interface MviKotlinHistoryIntent {
    data class ChangeCategory(val category: String?) : MviKotlinHistoryIntent
    data class SelectTransaction(val transaction: HistoryTransactionItem?) : MviKotlinHistoryIntent
    data object DismissDetail : MviKotlinHistoryIntent
    data object Refresh : MviKotlinHistoryIntent
}

/** Internal actions produced by bootstrapper. */
sealed interface MviKotlinHistoryAction {
    data object Init : MviKotlinHistoryAction
}

/** Internal state change messages consumed exclusively by Reducer. */
sealed interface MviKotlinHistoryMessage {
    data class SetLoading(val isLoading: Boolean) : MviKotlinHistoryMessage
    data class SetTransactions(val items: List<HistoryTransactionItem>) : MviKotlinHistoryMessage
    data class SetCategory(val category: String?) : MviKotlinHistoryMessage
    data class SetSelected(val transaction: HistoryTransactionItem?) : MviKotlinHistoryMessage
    data class SetError(val errorMessage: String?) : MviKotlinHistoryMessage
}

/** One-off side effect labels emitted to subscribers. */
sealed interface MviKotlinHistoryLabel {
    data class Toast(val message: String) : MviKotlinHistoryLabel
}

