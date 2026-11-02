package com.ledger.personal.feature.history.orbit

/** One-off side effects emitted by Orbit MVI history state container. */
sealed interface OrbitHistorySideEffect {
    data class ShowToast(val message: String) : OrbitHistorySideEffect
    data class TransactionClicked(val id: String) : OrbitHistorySideEffect
}

