package com.ledger.personal.ui.screens.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import com.ledger.personal.data.repository.FakeTransactionRepository
import com.ledger.personal.testing.FakeSyncCoordinator
import com.ledger.personal.testing.FakeUserSettings
import com.ledger.personal.testing.ScriptedTransactionRepository
import com.ledger.personal.testing.sampleTransaction
import com.ledger.personal.ui.screens.create.CreateTransactionViewModel
import com.ledger.personal.ui.screens.detail.TransactionDetailViewModel
import com.ledger.personal.ui.screens.edit.EditTransactionViewModel
import com.ledger.personal.ui.screens.list.TransactionListViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
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
    private lateinit var sync: FakeSyncCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeTransactionRepository()
        sync = FakeSyncCoordinator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listViewModel_loadsInitialTransactionsSuccessfully() = runTest {
        val viewModel = listViewModel(fakeRepository)
        val items = viewModel.pages.asSnapshot()

        assertEquals(25, items.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun listViewModel_showsEmptyStateWhenRepositoryIsEmpty() = runTest {
        fakeRepository.clear()
        val viewModel = listViewModel(fakeRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertTrue(state.isEmpty)
        assertTrue(state.isEndReached)
    }

    @Test
    fun listViewModel_showsEveryLocalRowAndIgnoresExtraPageRequests() = runTest {
        val viewModel = listViewModel(fakeRepository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        val items = viewModel.pages.asSnapshot()

        assertEquals(25, items.size)
        assertEquals(25, items.map { it.id }.distinct().size)
    }

    @Test
    fun listViewModel_refreshEnqueuesSyncAndKeepsNewLocalRows() = runTest {
        fakeRepository.clear()
        fakeRepository.insertDirect(sampleTransaction(id = "only"))
        val viewModel = listViewModel(fakeRepository)
        advanceUntilIdle()

        fakeRepository.insertDirect(sampleTransaction(id = "newer", timestampEpochMs = 9_000_000_000_000L))
        val enqueuedBefore = sync.enqueued
        viewModel.refresh()
        val items = viewModel.pages.asSnapshot()

        assertEquals("newer", items.first().id)
        assertTrue(sync.enqueued > enqueuedBefore)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun listViewModel_surfacesLoadError() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getTransactionsResult = Result.failure(IllegalStateException("timeout"))
        }
        val viewModel = listViewModel(repo)
        val failure = runCatching { viewModel.pages.asSnapshot() }
        assertTrue(failure.isFailure)
    }

    private fun TestScope.listViewModel(repository: com.ledger.personal.domain.repository.TransactionRepository): TransactionListViewModel {
        val viewModel = TransactionListViewModel(repository, sync, FakeUserSettings())
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TestScope.detailViewModel(
        repository: com.ledger.personal.domain.repository.TransactionRepository,
        savedStateHandle: androidx.lifecycle.SavedStateHandle,
    ): TransactionDetailViewModel {
        val viewModel = TransactionDetailViewModel(repository, savedStateHandle)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
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
        val viewModel = detailViewModel(fakeRepository, savedStateHandle)
        advanceUntilIdle()

        assertEquals(tx.id, viewModel.uiState.value.transaction?.id)

        viewModel.setDeleteDialogVisible(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertTrue(fakeRepository.getTransactionById(tx.id).isFailure)
    }

    @Test
    fun detailViewModel_missingIdSetsError() = runTest {
        val viewModel = detailViewModel(fakeRepository, SavedStateHandle())
        advanceUntilIdle()
        assertEquals("Missing transaction ID", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun detailViewModel_loadAndDeleteFailures() = runTest {
        val repo = ScriptedTransactionRepository().apply {
            getByIdResult = Result.failure(NoSuchElementException("gone"))
        }
        val viewModel = detailViewModel(repo, SavedStateHandle(mapOf("transactionId" to "gone")))
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
        val viewModel = detailViewModel(repo, SavedStateHandle(mapOf("transactionId" to "missing")))
        advanceUntilIdle()
        viewModel.confirmDelete()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isDeleted)
    }
}
