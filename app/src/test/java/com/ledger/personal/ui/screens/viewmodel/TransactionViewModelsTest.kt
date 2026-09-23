package com.ledger.personal.ui.screens.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.ledger.personal.data.repository.FakeTransactionRepository
import com.ledger.personal.testing.ScriptedTransactionRepository
import com.ledger.personal.testing.sampleTransaction
import com.ledger.personal.ui.screens.create.CreateTransactionViewModel
import com.ledger.personal.ui.screens.detail.TransactionDetailViewModel
import com.ledger.personal.ui.screens.edit.EditTransactionViewModel
import com.ledger.personal.ui.screens.list.TransactionListViewModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeTransactionRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTransactionRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listViewModel_loadsInitialTransactionsSuccessfully() = runTest {
        val viewModel = TransactionListViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(15, state.items.size)
        assertEquals(1, state.currentPage)
        assertFalse(state.isEndReached)
        assertNull(state.errorMessage)
    }

    @Test
    fun listViewModel_showsEmptyStateWhenRepositoryIsEmpty() = runTest {
        fakeRepository.clear()
        val viewModel = TransactionListViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertTrue(state.isEmpty)
        assertTrue(state.isEndReached)
    }

    @Test
    fun listViewModel_appendsNextPageAndStopsAtEnd() = runTest {
        val viewModel = TransactionListViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val afterSecond = viewModel.uiState.value
        assertEquals(25, afterSecond.items.size)
        assertEquals(2, afterSecond.currentPage)
        assertTrue(afterSecond.isEndReached)
        assertEquals(25, afterSecond.items.map { it.id }.distinct().size)

        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals(25, viewModel.uiState.value.items.size)
    }

    @Test
    fun listViewModel_refreshReloadsFirstPage() = runTest {
        fakeRepository.clear()
        fakeRepository.insertDirect(sampleTransaction(id = "only"))
        val viewModel = TransactionListViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.insertDirect(sampleTransaction(id = "newer", timestampEpochMs = 9_000_000_000_000L))
        viewModel.refresh()
        advanceUntilIdle()

        assertEquals("newer", viewModel.uiState.value.items.first().id)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun listViewModel_surfacesLoadError() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.failure(IllegalStateException("timeout"))
        }
        val viewModel = TransactionListViewModel(repo)
        advanceUntilIdle()
        assertEquals("timeout", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun listViewModel_surfacesRefreshAndNextPageErrors() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.success(
                List(15) { sampleTransaction(id = "tx-$it") }
            )
        }
        val viewModel = TransactionListViewModel(repo)
        advanceUntilIdle()

        repo.getTransactionsResult = Result.failure(IllegalStateException("refresh failed"))
        viewModel.refresh()
        advanceUntilIdle()
        assertEquals("refresh failed", viewModel.uiState.value.errorMessage)

        repo.getTransactionsResult = Result.failure(IllegalStateException("page failed"))
        viewModel.loadNextPage()
        advanceUntilIdle()
        assertEquals("page failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoadingNextPage)
    }

    @Test
    fun createViewModel_rejectsZeroAndNegativeAmount() = runTest {
        val viewModel = CreateTransactionViewModel(fakeRepository)
        viewModel.onAmountChange("0.00")
        viewModel.onCategoryChange("Food")
        viewModel.submit()

        val state = viewModel.uiState.value
        assertNotNull(state.amountError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun createViewModel_rejectsBlankCategory() = runTest {
        val viewModel = CreateTransactionViewModel(fakeRepository)
        viewModel.onAmountChange("10.00")
        viewModel.submit()
        assertEquals("Category is required", viewModel.uiState.value.categoryError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun createViewModel_createsValidTransaction() = runTest {
        val viewModel = CreateTransactionViewModel(fakeRepository)
        viewModel.onAmountChange("45.50")
        viewModel.onCategoryChange("Dining")
        viewModel.onDescriptionChange("Dinner with team")
        viewModel.submit()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.amountError)
        assertTrue(state.isSuccess)
    }

    @Test
    fun createViewModel_blankDescriptionBecomesUncategorized() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            createResult = Result.success(sampleTransaction())
        }
        val viewModel = CreateTransactionViewModel(repo)
        viewModel.onAmountChange("12.00")
        viewModel.onCategoryChange("  Food  ")
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1200L, repo.lastCreateAmount)
        assertEquals("Uncategorized", repo.lastCreateDescription)
        assertTrue(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun createViewModel_surfacesRepositoryFailure() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            createResult = Result.failure(IllegalStateException("save failed"))
        }
        val viewModel = CreateTransactionViewModel(repo)
        viewModel.onAmountChange("12.00")
        viewModel.onCategoryChange("Food")
        viewModel.submit()
        advanceUntilIdle()
        assertEquals("save failed", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSuccess)
        assertFalse(viewModel.uiState.value.isSubmitting)
    }

    @Test
    fun editViewModel_patchCategoryOnlyLeavesAmountUntouched() = runTest {
        val tx = sampleTransaction(
            id = "test-edit-1",
            amountInCents = 9900L,
            category = "OldCategory",
            description = "Original Description"
        )
        fakeRepository.insertDirect(tx)

        val savedStateHandle = SavedStateHandle(mapOf("transactionId" to tx.id))
        val viewModel = EditTransactionViewModel(fakeRepository, savedStateHandle)
        advanceUntilIdle()

        viewModel.onCategoryChange("UpdatedCategory")
        viewModel.submitPatch()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSuccess)
        val updated = fakeRepository.getTransactionById(tx.id).getOrThrow()
        assertEquals("UpdatedCategory", updated.category)
        assertEquals(9900L, updated.amountInCents)
    }

    @Test
    fun editViewModel_noChangesMarksSuccessWithoutPatch() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getByIdResult = Result.success(sampleTransaction(id = "same", amountInCents = 1500L))
        }
        val viewModel = EditTransactionViewModel(repo, SavedStateHandle(mapOf("transactionId" to "same")))
        advanceUntilIdle()
        viewModel.submitPatch()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isSuccess)
        assertNull(repo.lastPatchId)
    }

    @Test
    fun editViewModel_rejectsInvalidAmountAndBlankCategory() = runTest {
        fakeRepository.insertDirect(sampleTransaction(id = "edit-val"))
        val viewModel = EditTransactionViewModel(
            fakeRepository,
            SavedStateHandle(mapOf("transactionId" to "edit-val"))
        )
        advanceUntilIdle()
        viewModel.onAmountChange("0")
        viewModel.onCategoryChange(" ")
        viewModel.submitPatch()
        assertNotNull(viewModel.uiState.value.amountError)
        assertNotNull(viewModel.uiState.value.categoryError)
        assertFalse(viewModel.uiState.value.isSuccess)
    }

    @Test
    fun editViewModel_missingIdSetsError() = runTest {
        val viewModel = EditTransactionViewModel(fakeRepository, SavedStateHandle())
        assertEquals("Missing transaction ID", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoadingInitial)
    }

    @Test
    fun editViewModel_loadAndPatchFailuresSurfaceMessages() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getByIdResult = Result.failure(NoSuchElementException("not found"))
        }
        val viewModel = EditTransactionViewModel(repo, SavedStateHandle(mapOf("transactionId" to "x")))
        advanceUntilIdle()
        assertEquals("not found", viewModel.uiState.value.errorMessage)

        repo.getByIdResult = Result.success(sampleTransaction(id = "x"))
        viewModel.loadInitial("x")
        advanceUntilIdle()
        repo.patchResult = Result.failure(IllegalStateException("conflict"))
        viewModel.onDescriptionChange("changed")
        viewModel.submitPatch()
        advanceUntilIdle()
        assertEquals("conflict", viewModel.uiState.value.errorMessage)
        assertEquals("x", repo.lastPatchId)
        assertEquals("changed", repo.lastPatchDescription)
        assertNull(repo.lastPatchCategory)
        assertNull(repo.lastPatchAmount)
    }

    @Test
    fun detailViewModel_handlesDeleteConfirmationFlow() = runTest {
        val tx = sampleTransaction(id = "test-del-1", amountInCents = 1500L, category = "Subscriptions")
        fakeRepository.insertDirect(tx)

        val savedStateHandle = SavedStateHandle(mapOf("transactionId" to tx.id))
        val viewModel = TransactionDetailViewModel(fakeRepository, savedStateHandle)
        advanceUntilIdle()

        assertEquals(tx.id, viewModel.uiState.value.transaction?.id)

        viewModel.setDeleteDialogVisible(true)
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertTrue(fakeRepository.getTransactionById(tx.id).isFailure)
    }

    @Test
    fun detailViewModel_missingIdSetsError() = runTest {
        val viewModel = TransactionDetailViewModel(fakeRepository, SavedStateHandle())
        assertEquals("Missing transaction ID", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun detailViewModel_loadAndDeleteFailures() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getByIdResult = Result.failure(NoSuchElementException("gone"))
        }
        val viewModel = TransactionDetailViewModel(repo, SavedStateHandle(mapOf("transactionId" to "gone")))
        advanceUntilIdle()
        assertEquals("gone", viewModel.uiState.value.errorMessage)

        repo.getByIdResult = Result.success(sampleTransaction(id = "gone"))
        viewModel.loadTransaction("gone")
        advanceUntilIdle()
        repo.deleteResult = Result.failure(IllegalStateException("locked"))
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertEquals("locked", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isDeleted)
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    @Test
    fun detailViewModel_confirmDeleteWithoutTransactionIsNoOp() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getByIdResult = Result.failure(NoSuchElementException("missing"))
        }
        val viewModel = TransactionDetailViewModel(repo, SavedStateHandle(mapOf("transactionId" to "missing")))
        advanceUntilIdle()
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDeleted)
    }
}
