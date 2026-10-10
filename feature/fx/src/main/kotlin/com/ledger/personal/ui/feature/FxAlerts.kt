package com.ledger.personal.ui.feature

/** Values rendered by the fx screen. */
data class FxUiState(
    val isLoading: Boolean = false,
    val permissionDenied: Boolean = false,
    val firedCount: Int = 0,
    val status: String = "Watching",
    val pair: String = "USD/INR",
    val rateMinor: Long? = null,
    val thresholdMinor: Long = DEFAULT_THRESHOLD,
    val triggered: Boolean = false,
    val errorMessage: String? = null,
)

const val DEFAULT_THRESHOLD = 8_350L
