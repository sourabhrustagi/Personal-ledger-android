package com.ledger.personal.feature.history.decompose

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DecomposeHistoryComponentTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScriptedTransactionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.success(
                listOf(
                    Transaction("tx-1", 75_00L, "INR", "Food", "Chai", 1_000L),
                    Transaction("tx-2", 150_00L, "INR", "Entertainment", "Movie", 2_000L),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadsTransactionsIntoDecomposeValue() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val component = DefaultDecomposeHistoryComponent(
            componentContext = DefaultComponentContext(),
            repository = repository,
            coroutineScope = testScope,
        )
        advanceUntilIdle()

        val state = component.state.value
        assertFalse(state.isLoading)
        assertEquals(2, state.transactions.size)
        assertEquals("Chai", state.transactions[0].description)
    }

    @Test
    fun filtersTransactionsByCategory() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val component = DefaultDecomposeHistoryComponent(
            componentContext = DefaultComponentContext(),
            repository = repository,
            coroutineScope = testScope,
        )
        advanceUntilIdle()

        component.onCategorySelected("Entertainment")
        advanceUntilIdle()

        assertEquals("Entertainment", component.state.value.selectedCategory)
        assertEquals(1, component.state.value.filteredTransactions.size)
        assertEquals("tx-2", component.state.value.filteredTransactions[0].id)
    }

    @Test
    fun selectsAndDismissesTransaction() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val component = DefaultDecomposeHistoryComponent(
            componentContext = DefaultComponentContext(),
            repository = repository,
            coroutineScope = testScope,
        )
        advanceUntilIdle()

        val item = HistoryTransactionItem("tx-1", 75_00L, "INR", "Food", "Chai", 1_000L)
        component.onTransactionClicked(item)
        assertEquals(item, component.state.value.selectedTransaction)

        component.onDismissDetail()
        assertNull(component.state.value.selectedTransaction)
    }
}

