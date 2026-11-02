package com.ledger.personal.feature.history.orbit

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
class OrbitHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScriptedTransactionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.success(
                listOf(
                    Transaction("tx-1", 150_00L, "INR", "Food", "Lunch", 1_000L),
                    Transaction("tx-2", 400_00L, "INR", "Shopping", "Shoes", 2_000L),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTransactionsIntoOrbitState() = runTest(testDispatcher) {
        val viewModel = OrbitHistoryViewModel(repository)
        advanceUntilIdle()

        val state = viewModel.container.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals(2, state.transactions.size)
        assertEquals("Lunch", state.transactions[0].description)
    }

    @Test
    fun filtersTransactionsByCategory() = runTest(testDispatcher) {
        val viewModel = OrbitHistoryViewModel(repository)
        advanceUntilIdle()

        viewModel.selectCategory("Food")
        advanceUntilIdle()

        val state = viewModel.container.stateFlow.value
        assertEquals("Food", state.selectedCategory)
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("tx-1", state.filteredTransactions[0].id)
    }

    @Test
    fun selectsAndDismissesTransactionDetail() = runTest(testDispatcher) {
        val viewModel = OrbitHistoryViewModel(repository)
        advanceUntilIdle()

        val item = HistoryTransactionItem("tx-1", 150_00L, "INR", "Food", "Lunch", 1_000L)
        viewModel.selectTransaction(item)
        advanceUntilIdle()
        assertEquals(item, viewModel.container.stateFlow.value.selectedTransaction)

        viewModel.dismissDetail()
        advanceUntilIdle()
        assertNull(viewModel.container.stateFlow.value.selectedTransaction)
    }
}
