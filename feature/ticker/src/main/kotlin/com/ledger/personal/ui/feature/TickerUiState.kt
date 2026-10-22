package com.ledger.personal.ui.feature

/**
 * Immutable UI state representing the ticker / markets screen.
 */
data class TickerUiState(
    val tick: Long = 0L,
    val isPolling: Boolean = false,
)
