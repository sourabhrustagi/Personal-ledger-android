package com.ledger.personal.data.repository

import com.ledger.personal.core.TransactionRejected
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.testing.sampleTransaction
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class MockTransactionRepositoryTest {

    private lateinit var repository: MockTransactionRepository

    @Before
    fun setUp() {
        MockSwitches.flakyPayments.value = false
        MockSwitches.nextRolls.clear()
        repository = MockTransactionRepository()
        repository.clear()
    }

    @After
    fun tearDown() {
        MockSwitches.flakyPayments.value = false
        MockSwitches.nextRolls.clear()
    }

    @Test
    fun seedsTwentyFiveTransactionsByDefault() = runTest {
        val seeded = MockTransactionRepository()
        val page1 = seeded.getTransactions(page = 1, pageSize = 25).getOrThrow()
        val page2 = seeded.getTransactions(page = 2, pageSize = 25).getOrThrow()
        assertEquals(25, page1.size)
        assertTrue(page2.isEmpty())
    }

    @Test
    fun patchCategoryLeavesAmountUntouched() = runTest {
        val created = repository.createTransaction(
            amountInCents = 12500L,
            currency = "USD",
            category = "Utilities",
            description = "Electricity Bill"
        ).getOrThrow()

        val updated = repository.patchTransaction(
            id = created.id,
            category = "Office Expenses"
        ).getOrThrow()

        assertEquals("Office Expenses", updated.category)
        assertEquals(12500L, updated.amountInCents)
        assertEquals(created.id, updated.id)
        assertEquals("Electricity Bill", updated.description)
    }

    @Test
    fun patchDescriptionAndAmount() = runTest {
        val created = repository.createTransaction(500L, "USD", "Food", "Snack").getOrThrow()

        val updated = repository.patchTransaction(
            id = created.id,
            description = "Coffee",
            amountInCents = 750L
        ).getOrThrow()

        assertEquals("Food", updated.category)
        assertEquals("Coffee", updated.description)
        assertEquals(750L, updated.amountInCents)
    }

    @Test
    fun patchRejectsZeroAmount() = runTest {
        val created = repository.createTransaction(500L, "USD", "Food", "Snack").getOrThrow()
        val result = repository.patchTransaction(id = created.id, amountInCents = 0L)
        assertTrue(result.isFailure)
        assertEquals(500L, repository.getTransactionById(created.id).getOrThrow().amountInCents)
    }

    @Test
    fun patchMissingIdFails() = runTest {
        val result = repository.patchTransaction(id = "missing", category = "X")
        assertTrue(result.exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun listingIsPaginatedWithStableSortSoPageTwoNeverRepeatsPageOne() = runTest {
        for (i in 1..15) {
            repository.insertDirect(
                sampleTransaction(
                    id = "tx-$i",
                    amountInCents = 1000L * i,
                    category = "Category $i",
                    description = "Description $i",
                    timestampEpochMs = 1_710_000_000_000L + (i * 1000L)
                )
            )
        }

        val page1 = repository.getTransactions(page = 1, pageSize = 10).getOrThrow()
        val page2 = repository.getTransactions(page = 2, pageSize = 10).getOrThrow()

        assertEquals(10, page1.size)
        assertEquals(5, page2.size)
        assertTrue(page1.map { it.id }.toSet().intersect(page2.map { it.id }.toSet()).isEmpty())
        assertEquals(15, (page1 + page2).map { it.id }.distinct().size)
        assertEquals(page1.map { it.timestampEpochMs }, page1.map { it.timestampEpochMs }.sortedDescending())
    }

    @Test
    fun sameTimestampSortsByIdDescending() = runTest {
        val ts = 1_710_000_000_000L
        repository.insertDirect(sampleTransaction(id = "tx-a", timestampEpochMs = ts))
        repository.insertDirect(sampleTransaction(id = "tx-c", timestampEpochMs = ts))
        repository.insertDirect(sampleTransaction(id = "tx-b", timestampEpochMs = ts))

        val ids = repository.getTransactions(page = 1, pageSize = 10).getOrThrow().map { it.id }
        assertEquals(listOf("tx-c", "tx-b", "tx-a"), ids)
    }

    @Test
    fun pagePastEndReturnsEmptyList() = runTest {
        repository.insertDirect(sampleTransaction())
        val page = repository.getTransactions(page = 5, pageSize = 10).getOrThrow()
        assertTrue(page.isEmpty())
    }

    @Test
    fun invalidPageArgumentsThrow() = runTest {
        try {
            repository.getTransactions(page = 0, pageSize = 10)
            throw AssertionError("expected require failure")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("Page number"))
        }
        try {
            repository.getTransactions(page = 1, pageSize = 0)
            throw AssertionError("expected require failure")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty().contains("Page size"))
        }
    }

    @Test
    fun getByIdSucceedsAndFails() = runTest {
        repository.insertDirect(sampleTransaction(id = "found"))
        assertEquals("found", repository.getTransactionById("found").getOrThrow().id)
        assertTrue(repository.getTransactionById("missing").exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun createRejectsInvalidAmount() = runTest {
        val result = repository.createTransaction(0L, "USD", "Food", "Bad")
        assertTrue(result.isFailure)
    }

    @Test
    fun deleteRemovesRowAndFailsWhenMissing() = runTest {
        repository.insertDirect(sampleTransaction(id = "gone"))
        assertTrue(repository.deleteTransaction("gone").isSuccess)
        assertTrue(repository.getTransactionById("gone").isFailure)
        assertTrue(repository.deleteTransaction("gone").exceptionOrNull() is NoSuchElementException)
    }

    @Test
    fun insertDirectOverwritesExistingId() = runTest {
        repository.insertDirect(sampleTransaction(id = "dup", amountInCents = 100L))
        repository.insertDirect(sampleTransaction(id = "dup", amountInCents = 200L, category = "Rent"))
        val stored = repository.getTransactionById("dup").getOrThrow()
        assertEquals(200L, stored.amountInCents)
        assertEquals("Rent", stored.category)
    }

    @Test
    fun createRetriesACancelledWriteThenSucceeds() = runTest {
        MockSwitches.flakyPayments.value = true
        MockSwitches.nextRolls.addLast(0)
        MockSwitches.nextRolls.addLast(80)
        val created = repository.createTransaction(500L, "INR", "Food", "Tea").getOrThrow()
        assertEquals("Food", created.category)
        assertEquals(1, repository.getTransactions(1, 10).getOrThrow().size)
    }

    @Test
    fun createDoesNotRetryAServerRejection() = runTest {
        MockSwitches.flakyPayments.value = true
        MockSwitches.nextRolls.addLast(30)
        val result = repository.createTransaction(500L, "INR", "Food", "Tea")
        assertTrue(result.exceptionOrNull() is TransactionRejected)
        assertTrue(repository.getTransactions(1, 10).getOrThrow().isEmpty())
    }

    @Test
    fun createGivesUpAfterDroppedConnections() = runTest {
        MockSwitches.flakyPayments.value = true
        repeat(3) { MockSwitches.nextRolls.addLast(0) }
        val result = repository.createTransaction(500L, "INR", "Food", "Tea")
        assertTrue(result.exceptionOrNull() is IOException)
        assertTrue(repository.getTransactions(1, 10).getOrThrow().isEmpty())
    }
}
