package com.ledger.personal.ui.feature

/** Allowed values of chart tab. */
enum class ChartTab(val label: String, val section: String) {
    MONTH("By month", "Months"),
    CATEGORY("By category", "Categories"),
}

/** Slice. */
data class Slice(val label: String, val amountMinor: Long, val count: Int = 0)

/** Values rendered by the charts screen. */
data class ChartsUiState(
    val isLoading: Boolean = true,
    val tab: ChartTab = ChartTab.CATEGORY,
    val currency: String = "INR",
    val slices: List<Slice> = emptyList(),
    val categories: List<String> = emptyList(),
    val category: String = "All",
    val incomeMinor: Long = 0L,
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean get() = !isLoading && errorMessage == null && slices.isEmpty()
    val totalMinor: Long get() = slices.sumOf { it.amountMinor }
}
