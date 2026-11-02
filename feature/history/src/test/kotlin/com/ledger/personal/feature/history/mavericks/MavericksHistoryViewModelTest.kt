package com.ledger.personal.feature.history.mavericks

import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.testing.ScriptedTransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MavericksHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScriptedTransactionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.success(
                listOf(
                    Transaction("tx-1", 100_00L, "INR", "Food", "Groceries", 1_000L),
                    Transaction("tx-2", 200_00L, "INR", "Shopping", "Clothes", 2_000L),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun executesAsyncLoadingIntoSuccess() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val viewModel = MavericksHistoryViewModel(repository, testScope)
        advanceUntilIdle()

        val state = viewModel.stateFlow.value
        assertTrue(state.transactions is Async.Success)
        assertEquals(2, state.transactions()?.size)
        assertEquals("Groceries", state.transactions()?.get(0)?.description)
    }

    @Test
    fun filtersTransactionsByCategory() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val viewModel = MavericksHistoryViewModel(repository, testScope)
        advanceUntilIdle()

        viewModel.selectCategory("Shopping")
        advanceUntilIdle()

        assertEquals("Shopping", viewModel.stateFlow.value.selectedCategory)
        assertEquals(1, viewModel.stateFlow.value.filteredTransactions.size)
        assertEquals("tx-2", viewModel.stateFlow.value.filteredTransactions[0].id)
    }

    @Test
    fun selectsAndDismissesTransaction() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val viewModel = MavericksHistoryViewModel(repository, testScope)
        advanceUntilIdle()

        val item = HistoryTransactionItem("tx-1", 100_00L, "INR", "Food", "Groceries", 1_000L)
        viewModel.selectTransaction(item)
        advanceUntilIdle()
        assertEquals(item, viewModel.stateFlow.value.selectedTransaction)

        viewModel.dismissDetail()
        advanceUntilIdle()
        assertNull(viewModel.stateFlow.value.selectedTransaction)
    }
}

