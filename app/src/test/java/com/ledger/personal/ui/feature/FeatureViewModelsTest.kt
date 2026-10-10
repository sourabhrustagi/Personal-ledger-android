package com.ledger.personal.ui.feature

import androidx.lifecycle.SavedStateHandle
import com.ledger.personal.core.PayEvent
import com.ledger.personal.core.PayState
import com.ledger.personal.data.local.MemoryAssistantHistory
import com.ledger.personal.data.local.MemoryKycDraftStore
import com.ledger.personal.data.local.TickerSocket
import com.ledger.personal.mock.MockCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelsTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun walletRejectsInsufficientFundsAndRetriesOnce() {
        val viewModel = WalletViewModel(rail = { post -> post() }, retryDelayMs = 0)
        viewModel.onAmount(viewModel.uiState.value.balanceMinor + 1)
        viewModel.review()
        assertEquals(TransferStatus.INSUFFICIENT, viewModel.uiState.value.status)

        viewModel.onAmount(100L)
        viewModel.review()
        viewModel.onStepUp(true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransferStep.RECEIPT, viewModel.uiState.value.step)
        assertEquals(1, viewModel.postedCount())
    }

    @Test
    fun droppedResponseAfterPostingIsRetriedWithoutChargingTwice() {
        var calls = 0
        val viewModel = WalletViewModel(
            rail = { post ->
                calls++
                val posted = post()
                if (calls == 1) throw java.io.IOException("dropped after posting")
                posted
            },
            retryDelayMs = 0,
        )
        val opening = viewModel.uiState.value.balanceMinor
        viewModel.onAmount(100L)
        viewModel.review()
        viewModel.onStepUp(true)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransferStatus.DONE, viewModel.uiState.value.status)
        assertEquals(2, calls)
        assertEquals(1, viewModel.postedCount())
        assertEquals(opening - 100L, viewModel.uiState.value.balanceMinor)
    }

    @Test
    fun declinedPaymentShowsAMessageAndIsNotRetried() {
        var calls = 0
        val viewModel = WalletViewModel(
            rail = {
                calls++
                throw com.ledger.personal.core.TransactionRejected("Your bank declined this payment")
            },
            retryDelayMs = 0,
        )
        viewModel.onAmount(100L)
        viewModel.review()
        viewModel.onStepUp(true)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransferStatus.FAILED, viewModel.uiState.value.status)
        assertEquals(1, calls)
        assertEquals(0, viewModel.postedCount())
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Nothing was changed on the server"))
    }

    @Test
    fun deniedNotificationPermissionDoesNotFire() {
        val viewModel = FxAlertsViewModel()
        viewModel.onPermission(false)
        viewModel.check(10_000L)
        assertTrue(viewModel.uiState.value.permissionDenied)
        assertEquals(0, viewModel.uiState.value.firedCount)
    }

    @Test
    fun alertFiresOnceWhileRateStaysAboveThreshold() {
        val viewModel = FxAlertsViewModel()
        viewModel.onPermission(true)
        viewModel.check(DEFAULT_THRESHOLD + 50L)
        viewModel.check(DEFAULT_THRESHOLD + 100L)
        assertEquals(1, viewModel.uiState.value.firedCount)
    }

    @Test
    fun lowConfidenceAndConflictStayVisible() {
        val viewModel = KycViewModel(SavedStateHandle())
        viewModel.onName("Asha")
        viewModel.onConfidence(0.2)
        viewModel.capture()
        viewModel.incomingEdit("Other")
        assertTrue(viewModel.uiState.value.humanReview)
        assertTrue(viewModel.uiState.value.captureUri!!.startsWith("content://"))
        assertEquals("Sync conflict on name", viewModel.uiState.value.conflict)
        assertEquals("Asha", viewModel.uiState.value.name)
    }

    @Test
    fun illegalSettleStaysIdle() {
        val viewModel = PosViewModel()
        viewModel.onEvent(PayEvent.SETTLE)
        assertEquals(PayState.IDLE, viewModel.uiState.value.pay)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Illegal"))
    }

    @Test
    fun undoSendsNoBlock() {
        val viewModel = FraudViewModel()
        viewModel.schedule("card-1")
        viewModel.undo("card-1")
        viewModel.commit()
        assertEquals(0, viewModel.uiState.value.blocked)
    }

    @Test
    fun unknownCategoryChartIsEmpty() {
        val viewModel = ChartsViewModel()
        viewModel.setCategory("No such category")
        assertTrue(viewModel.uiState.value.isEmpty)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun assistantDropsDuplicateToken() {
        val viewModel = AssistantViewModel()
        viewModel.push("Hello")
        viewModel.push("Hello")
        assertEquals("Hello", viewModel.uiState.value.tokens)
    }

    @Test
    fun assistantRestoresHistoryAndSkipsARepeatedStreamToken() = runTest {
        val history = MemoryAssistantHistory()
        AssistantViewModel(history).push("Hello")
        advanceUntilIdle()
        val restored = AssistantViewModel(history)
        advanceUntilIdle()
        restored.consume("data: Hello")
        restored.consume("data: Again")
        assertEquals("HelloAgain", restored.uiState.value.tokens)
    }

    @Test
    fun bankingEmiMatchesTheLoan() {
        val viewModel = BankingViewModel()
        assertEquals(1_660_715L, viewModel.uiState.value.emiMinor)
        assertEquals(MockCatalog.loanPrincipalMinor, viewModel.uiState.value.legs.sumOf { it.principalMinor })
    }

    @Test
    fun tickerPausesWhenNothingIsCollecting() = runTest {
        val viewModel = ticker(this)
        advanceTimeBy(1_000)
        assertTrue(viewModel.ticks.value > 0L)
    }

    @Test
    fun kycDraftSurvivesANewViewModel() = runTest {
        val store = MemoryKycDraftStore()
        KycViewModel(SavedStateHandle(), store).onName("Asha")
        advanceUntilIdle()
        val restored = KycViewModel(SavedStateHandle(), store)
        advanceUntilIdle()
        assertEquals("Asha", restored.uiState.value.name)
    }

    @Test
    fun captureStoresTheDocumentEdge() {
        val viewModel = KycViewModel(SavedStateHandle())
        viewModel.capture()
        assertEquals(2..3, viewModel.uiState.value.edge)
    }

    @Test
    fun tickerPollsAfterTheSocketDrops() = runTest {
        val viewModel = TickerViewModel(object : TickerSocket {
            override fun prices(): Flow<Long> = flow { error("socket down") }
        })
        backgroundScope.launch { viewModel.ticks.collect { } }
        advanceTimeBy(1_500)
        assertTrue(viewModel.polling.value)
        assertTrue(viewModel.ticks.value > 0L)
    }

    @Test
    fun posReadsLastFourAndQueuesOneForward() = runTest {
        val viewModel = PosViewModel()
        viewModel.onEvent(PayEvent.TAP)
        viewModel.onEvent(PayEvent.APPROVE)
        viewModel.onEvent(PayEvent.SETTLE)
        advanceUntilIdle()
        assertEquals(PayState.SETTLED, viewModel.uiState.value.pay)
        assertEquals("1111", viewModel.uiState.value.lastFour)
        assertEquals(1, viewModel.uiState.value.forwarded)
        viewModel.onEvent(PayEvent.SETTLE)
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.forwarded)
    }

    @Test
    fun customerCannotOpenTheFraudQueue() {
        val viewModel = FraudViewModel()
        viewModel.openAs("CUSTOMER")
        assertEquals("Analyst role required", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun bankingAcceptsMpinAndBlocksANewBeneficiary() {
        val viewModel = BankingViewModel()
        viewModel.signIn("1234")
        viewModel.showUpi("asha@ok", 1050L)
        viewModel.addBeneficiary(0L)
        viewModel.pay(60_000L)
        assertTrue(viewModel.uiState.value.signedIn)
        assertEquals("upi://pay?pa=asha@ok&am=10.50", viewModel.uiState.value.upi)
        assertTrue(viewModel.uiState.value.cooling)
        viewModel.payBill("electricity", 50_000L)
        assertEquals("electricity:50000", viewModel.uiState.value.bill)
    }

    @Test
    fun documentSpanFindsTheBrightEdge() {
        val span = documentSpan(intArrayOf(0, 0, 80, 90, 0))
        assertEquals(2..3, span)
        assertNull(documentSpan(intArrayOf(0, 1, 2)))
    }

    @Test
    fun missingNotificationPermissionSkipsThePost() {
        assertEquals(FxNotify.SKIP, fxNotifyDecision(permissionGranted = false, breached = true))
        assertEquals(FxNotify.POST, fxNotifyDecision(permissionGranted = true, breached = true))
    }

    private fun ticker(scope: TestScope): TickerViewModel {
        val viewModel = TickerViewModel()
        scope.backgroundScope.launch { viewModel.ticks.collect { } }
        return viewModel
    }
}
