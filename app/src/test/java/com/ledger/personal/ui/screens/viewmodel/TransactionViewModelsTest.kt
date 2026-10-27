package com.ledger.personal.ui.screens.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.paging.testing.asSnapshot
import com.ledger.personal.core.transactionFailureMessage
import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.testing.MockSyncCoordinator
import com.ledger.personal.testing.MockUserSettings
import com.ledger.personal.testing.ScriptedTransactionRepository
import com.ledger.personal.testing.sampleTransaction
import com.ledger.personal.ui.screens.detail.TransactionDetailViewModel
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
    private lateinit var mockRepository: MockTransactionRepository
    private lateinit var sync: MockSyncCoordinator

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        MockSwitches.flakyPayments.value = false
        MockSwitches.nextRolls.clear()
        mockRepository = MockTransactionRepository()
        sync = MockSyncCoordinator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun listViewModel_loadsInitialTransactionsSuccessfully() = runTest {
        val viewModel = listViewModel(mockRepository)
        val items = viewModel.pages.asSnapshot()

        assertEquals(25, items.size)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun listViewModel_showsEmptyStateWhenRepositoryIsEmpty() = runTest {
        mockRepository.clear()
        val viewModel = listViewModel(mockRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.items.isEmpty())
        assertTrue(state.isEmpty)
        assertTrue(state.isEndReached)
    }

    @Test
    fun listViewModel_showsEveryLocalRowAndIgnoresExtraPageRequests() = runTest {
        val viewModel = listViewModel(mockRepository)
        advanceUntilIdle()

        viewModel.loadNextPage()
        val items = viewModel.pages.asSnapshot()

        assertEquals(25, items.size)
        assertEquals(25, items.map { it.id }.distinct().size)
    }

    @Test
    fun listViewModel_refreshEnqueuesSyncAndKeepsNewLocalRows() = runTest {
        mockRepository.clear()
        mockRepository.insertDirect(sampleTransaction(id = "only"))
        val viewModel = listViewModel(mockRepository)
        advanceUntilIdle()

        mockRepository.insertDirect(sampleTransaction(id = "newer", timestampEpochMs = 9_000_000_000_000L))
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
        val viewModel = TransactionListViewModel(repository, sync, MockUserSettings())
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
    fun detailViewModel_handlesDeleteConfirmationFlow() = runTest {
        val tx = sampleTransaction(id = "test-del-1", amountInCents = 1500L, category = "Subscriptions")
        mockRepository.insertDirect(tx)

        val savedStateHandle = SavedStateHandle(mapOf("transactionId" to tx.id))
        val viewModel = detailViewModel(mockRepository, savedStateHandle)
        advanceUntilIdle()

        assertEquals(tx.id, viewModel.uiState.value.transaction?.id)

        viewModel.setDeleteDialogVisible(true)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        viewModel.confirmDelete()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isDeleted)
        assertTrue(mockRepository.getTransactionById(tx.id).isFailure)
    }

    @Test
    fun detailViewModel_missingIdSetsError() = runTest {
        val viewModel = detailViewModel(mockRepository, SavedStateHandle())
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
        assertEquals(transactionFailureMessage(IllegalStateException("locked")), viewModel.uiState.value.errorMessage)
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
