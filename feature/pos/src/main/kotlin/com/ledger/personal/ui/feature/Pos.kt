package com.ledger.personal.ui.feature

import com.ledger.personal.core.PayState

/** Values rendered by the pos screen. */
data class PosUiState(
    val isLoading: Boolean = false,
    val pay: PayState = PayState.IDLE,
    val amountMinor: Long = 0L,
    val sale: Int = 1,
    val lastFour: String? = null,
    val forwarded: Int = 0,
    val pendingCount: Int = 0,
    val pendingTotalMinor: Long = 0L,
    val refunded: Boolean = false,
    val settlement: String? = null,
    val errorMessage: String? = null,
) {
    val reference: String get() = "pos-sale-$sale"
}
