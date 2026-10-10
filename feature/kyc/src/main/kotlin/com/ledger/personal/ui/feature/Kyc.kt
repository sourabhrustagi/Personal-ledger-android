package com.ledger.personal.ui.feature

/** Values rendered by the kyc screen. */
data class KycUiState(
    val isLoading: Boolean = false,
    val name: String = "",
    val confidence: Double = 1.0,
    val humanReview: Boolean = false,
    val captureUri: String? = null,
    val edge: IntRange? = null,
    val conflict: String? = null,
    val errorMessage: String? = null,
)

/** Document span. */
fun documentSpan(luma: IntArray, threshold: Int = 40): IntRange? {
    val start = luma.indexOfFirst { it >= threshold }
    if (start < 0) return null
    return start..luma.indexOfLast { it >= threshold }
}
