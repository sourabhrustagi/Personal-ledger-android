package com.ledger.personal.feature.history.voyager

import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.testing.ScriptedTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VoyagerHistoryScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScriptedTransactionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.success(
                listOf(
                    Transaction("tx-1", 120_00L, "INR", "Transfer", "Rent", 1_000L),
                    Transaction("tx-2", 30_00L, "INR", "Bills", "Water", 2_000L),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTransactionsIntoVoyagerState() = runTest(testDispatcher) {
        val model = VoyagerHistoryScreenModel(repository)
        advanceUntilIdle()

        val state = model.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.transactions.size)
        assertEquals("Rent", state.transactions[0].description)
    }

    @Test
    fun filtersTransactionsByCategory() = runTest(testDispatcher) {
        val model = VoyagerHistoryScreenModel(repository)
        advanceUntilIdle()

        model.selectCategory("Bills")
        advanceUntilIdle()

        assertEquals("Bills", model.state.value.selectedCategory)
        assertEquals(1, model.state.value.filteredTransactions.size)
        assertEquals("tx-2", model.state.value.filteredTransactions[0].id)
    }

    @Test
    fun handlesTransactionSelectionAndDismissal() = runTest(testDispatcher) {
        val model = VoyagerHistoryScreenModel(repository)
        advanceUntilIdle()

        val item = HistoryTransactionItem("tx-1", 120_00L, "INR", "Transfer", "Rent", 1_000L)
        model.selectTransaction(item)
        advanceUntilIdle()
        assertEquals(item, model.state.value.selectedTransaction)

        model.dismissDetail()
        advanceUntilIdle()
        assertNull(model.state.value.selectedTransaction)
    }
}
