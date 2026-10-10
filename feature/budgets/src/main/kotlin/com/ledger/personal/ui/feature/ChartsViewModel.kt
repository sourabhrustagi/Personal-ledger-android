package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import com.ledger.personal.mock.MockCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** State and actions for the charts screen. */
class ChartsViewModel : ViewModel() {
    private val state = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = state.asStateFlow()

    init {
        publish()
    }

    /** Select tab. */
    fun selectTab(tab: ChartTab) {
        state.update { it.copy(tab = tab) }
        publish()
    }

    /** Select currency. */
    fun selectCurrency(currency: String) {
        state.update { it.copy(currency = currency, category = "All") }
        publish()
    }

    /** Stores category. */
    fun setCategory(category: String) {
        state.update { it.copy(category = category) }
        publish()
    }

    /** Publish. */
    private fun publish() {
        val current = state.value
        val inCurrency = MockCatalog.transactions.filter { it.currency == current.currency }
        val spending = inCurrency.filter { it.category != INCOME }
        val rows = spending.filter { current.category == "All" || it.category == current.category }
        val slices = when (current.tab) {
            ChartTab.CATEGORY -> rows.groupBy { it.category }
                .map { (label, items) -> Slice(label, items.sumOf { it.amountMinor }, items.size) }
                .sortedByDescending { it.amountMinor }
            ChartTab.MONTH -> rows.groupBy { it.dayOffset / 30 }.toSortedMap()
                .map { (bucket, items) -> Slice(monthLabel(bucket), items.sumOf { it.amountMinor }, items.size) }
        }
        state.update {
            it.copy(
                isLoading = false,
                slices = slices,
                categories = spending.map { row -> row.category }.distinct().sorted(),
                incomeMinor = inCurrency.filter { row -> row.category == INCOME }.sumOf { row -> row.amountMinor },
                errorMessage = null,
            )
        }
    }

    /** Month label. */
    private fun monthLabel(monthsAgo: Int): String =
        YearMonth.now().minusMonths(monthsAgo.toLong()).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))

    private companion object {
        const val INCOME = "Salary"
    }
}
