package com.ledger.personal.feature.history.circuit

import com.ledger.personal.feature.history.model.HistoryTransactionItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CircuitHistoryStateTest {

    private val sampleItems = listOf(
        HistoryTransactionItem("tx-1", 250_00L, "INR", "Shopping", "Book", 1_000L),
        HistoryTransactionItem("tx-2", 300_00L, "INR", "Salary", "Bonus", 2_000L),
    )

    @Test
    fun propagatesEventsViaEventSink() {
        val capturedEvents = mutableListOf<CircuitHistoryEvent>()
        val state = CircuitHistoryUiState(
            isLoading = false,
            transactions = sampleItems,
            eventSink = { capturedEvents.add(it) },
        )

        state.eventSink(CircuitHistoryEvent.ChangeFilter("Salary"))
        state.eventSink(CircuitHistoryEvent.SelectTransaction(sampleItems.first()))
        state.eventSink(CircuitHistoryEvent.DismissDetail)
        state.eventSink(CircuitHistoryEvent.Refresh)

        assertEquals(4, capturedEvents.size)
        assertEquals(CircuitHistoryEvent.ChangeFilter("Salary"), capturedEvents[0])
        assertEquals(CircuitHistoryEvent.DismissDetail, capturedEvents[2])
    }

    @Test
    fun filtersTransactionsCorrectly() {
        val state = CircuitHistoryUiState(
            transactions = sampleItems,
            selectedCategory = "Shopping",
            eventSink = {},
        )
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("tx-1", state.filteredTransactions[0].id)
    }
}

