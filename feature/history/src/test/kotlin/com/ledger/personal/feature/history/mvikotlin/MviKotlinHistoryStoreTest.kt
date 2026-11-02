package com.ledger.personal.feature.history.mvikotlin

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
class MviKotlinHistoryStoreTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScriptedTransactionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.success(
                listOf(
                    Transaction("tx-1", 500_00L, "INR", "Salary", "Paycheck", 1_000L),
                    Transaction("tx-2", 60_00L, "INR", "Food", "Coffee", 2_000L),
                ),
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun bootstrapsAndLoadsTransactions() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val store = MviKotlinHistoryStoreFactory.create(repository, testScope)
        advanceUntilIdle()

        val state = store.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals(2, state.transactions.size)
        assertEquals("Paycheck", state.transactions[0].description)
    }

    @Test
    fun executesChangeCategoryIntent() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val store = MviKotlinHistoryStoreFactory.create(repository, testScope)
        advanceUntilIdle()

        store.accept(MviKotlinHistoryIntent.ChangeCategory("Salary"))
        advanceUntilIdle()

        val state = store.stateFlow.value
        assertEquals("Salary", state.selectedCategory)
        assertEquals(1, state.filteredTransactions.size)
        assertEquals("tx-1", state.filteredTransactions[0].id)
    }

    @Test
    fun executesSelectAndDismissIntent() = runTest(testDispatcher) {
        val testScope = TestScope(testDispatcher)
        val store = MviKotlinHistoryStoreFactory.create(repository, testScope)
        advanceUntilIdle()

        val item = HistoryTransactionItem("tx-2", 60_00L, "INR", "Food", "Coffee", 2_000L)
        store.accept(MviKotlinHistoryIntent.SelectTransaction(item))
        advanceUntilIdle()
        assertEquals(item, store.stateFlow.value.selectedTransaction)

        store.accept(MviKotlinHistoryIntent.DismissDetail)
        advanceUntilIdle()
        assertNull(store.stateFlow.value.selectedTransaction)
    }
}
