package com.ledger.personal.feature.history.molecule

import com.ledger.personal.feature.history.model.HistoryTransactionItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoleculeHistoryStateTest {

    private val sampleItems = listOf(
        HistoryTransactionItem("tx-1", 100_00L, "INR", "Food", "Biryani", 1_000L),
        HistoryTransactionItem("tx-2", 500_00L, "INR", "Bills", "Electricity", 2_000L),
    )

    @Test
    fun returnsAllTransactionsWhenCategoryIsNull() {
        val state = MoleculeHistoryState(
            isLoading = false,
            transactions = sampleItems,
            selectedCategory = null,
        )
        assertEquals(2, state.filteredTransactions.size)
    }

    @Test
    fun filtersTransactionsWhenCategoryIsSpecified() {
        val state = MoleculeHistoryState(
            isLoading = false,
            transactions = sampleItems,
            selectedCategory = "Bills",
        )
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("tx-2", state.filteredTransactions[0].id)
    }

    @Test
    fun modelsSelectedTransaction() {
        val selected = sampleItems.first()
        val state = MoleculeHistoryState(
            transactions = sampleItems,
            selectedTransaction = selected,
        )
        assertEquals(selected, state.selectedTransaction)

        val dismissed = state.copy(selectedTransaction = null)
        assertNull(dismissed.selectedTransaction)
    }
}
