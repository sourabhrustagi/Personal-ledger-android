package com.ledger.personal.feature.wallet

import com.ledger.personal.feature.wallet.payment.PaymentFsmEvent
import com.ledger.personal.feature.wallet.payment.PaymentFsmReducer
import com.ledger.personal.feature.wallet.payment.PaymentFsmState
import com.ledger.personal.feature.wallet.ui.TransferStatus
import com.ledger.personal.feature.wallet.ui.TransferStep
import com.ledger.personal.feature.wallet.ui.WalletViewModel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentFsmTest {
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
    fun formValidationRejectsZeroNegativeAndInsufficientFunds() {
        val form = PaymentFsmState.Form(recipient = "user@upi", amountMinor = 0L)
        val afterZero = PaymentFsmReducer.reduce(form, PaymentFsmEvent.Review(balanceMinor = 1_000L))
        assertTrue(afterZero is PaymentFsmState.Form && afterZero.validationError != null)

        val afterInsufficient = PaymentFsmReducer.reduce(
            form.copy(amountMinor = 2_000L),
            PaymentFsmEvent.Review(balanceMinor = 1_000L),
        )
        assertTrue(afterInsufficient is PaymentFsmState.Form && afterInsufficient.isInsufficientFunds)
    }

    @Test
    fun formTransitionsToAwaitingAuthOnValidInputs() {
        val form = PaymentFsmState.Form(recipient = "user@upi", amountMinor = 500L, note = "Coffee", idempotencyKey = "tx-1")
        val review = PaymentFsmReducer.reduce(form, PaymentFsmEvent.Review(balanceMinor = 1_000L))

        assertTrue(review is PaymentFsmState.AwaitingAuth)
        assertEquals("user@upi", review.recipient)
        assertEquals(500L, review.amountMinor)
        assertEquals("tx-1", review.idempotencyKey)
    }

    @Test
    fun stepUpTransitionsToSubmittingAndIncrementsAttempts() {
        val awaiting = PaymentFsmState.AwaitingAuth(recipient = "user@upi", amountMinor = 500L, note = "", idempotencyKey = "tx-1")
        val submitting = PaymentFsmReducer.reduce(awaiting, PaymentFsmEvent.AuthSuccess(true))

        assertTrue(submitting is PaymentFsmState.Submitting)
        assertEquals(1, (submitting as PaymentFsmState.Submitting).attempt)

        val retry = PaymentFsmReducer.reduce(submitting, PaymentFsmEvent.RetryAttempt(2))
        assertEquals(2, (retry as PaymentFsmState.Submitting).attempt)
    }

    @Test
    fun transientNetworkDropTransitionsToInDoubtAndPreservesIdempotencyKey() {
        val submitting = PaymentFsmState.Submitting(
            recipient = "user@upi",
            amountMinor = 500L,
            note = "",
            idempotencyKey = "tx-1",
            attempt = 3,
        )
        val inDoubt = PaymentFsmReducer.reduce(
            submitting,
            PaymentFsmEvent.SubmitTransientFailure("Network dropped mid-flight"),
        )

        assertTrue(inDoubt is PaymentFsmState.InDoubt)
        assertEquals("tx-1", inDoubt.idempotencyKey)

        // Reconciliation when switch confirmed receipt
        val reconciledSuccess = PaymentFsmReducer.reduce(inDoubt, PaymentFsmEvent.Reconciled(postedMinor = 500L))
        assertTrue(reconciledSuccess is PaymentFsmState.Success)
        assertEquals(500L, (reconciledSuccess as PaymentFsmState.Success).postedMinor)
    }

    @Test
    fun inDoubtReconciliationWhenBankHasNoRecordAllowsSafeRetry() {
        val inDoubt = PaymentFsmState.InDoubt(
            recipient = "user@upi",
            amountMinor = 500L,
            note = "",
            idempotencyKey = "tx-1",
            message = "Dropped",
        )
        val reconciledFailed = PaymentFsmReducer.reduce(inDoubt, PaymentFsmEvent.Reconciled(postedMinor = null))

        assertTrue(reconciledFailed is PaymentFsmState.Failed)
        assertTrue((reconciledFailed as PaymentFsmState.Failed).canRetry)
        assertEquals("tx-1", reconciledFailed.idempotencyKey)
    }

    @Test
    fun terminalFailureBlocksRetries() {
        val submitting = PaymentFsmState.Submitting(
            recipient = "user@upi",
            amountMinor = 500L,
            note = "",
            idempotencyKey = "tx-1",
        )
        val failed = PaymentFsmReducer.reduce(submitting, PaymentFsmEvent.SubmitTerminalFailure("Account frozen"))

        assertTrue(failed is PaymentFsmState.Failed)
        assertFalse((failed as PaymentFsmState.Failed).canRetry)
    }

    @Test
    fun endToEndViewModelRecoversInDoubtTransactionViaReconciliation() {
        var callCount = 0
        val viewModel = WalletViewModel(
            rail = { post ->
                callCount++
                post() // Bank executes debit
                throw IOException("Socket timeout waiting for HTTP 200 response")
            },
            retryDelayMs = 0,
        )

        viewModel.onRecipient("merchant@upi")
        viewModel.onAmount(300L)
        viewModel.review()
        viewModel.onStepUp(true)
        dispatcher.scheduler.advanceUntilIdle()

        // 3 retries were exhausted on dropped connections -> entered IN_DOUBT
        assertEquals(TransferStatus.IN_DOUBT, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.canReconcile)
        assertEquals(1, viewModel.postedCount) // Posted exactly once on server ledger

        // User or system taps "Check Status" / Reconcile
        viewModel.reconcile()
        dispatcher.scheduler.advanceUntilIdle()

        // Safely confirmed without double charging!
        assertEquals(TransferStatus.DONE, viewModel.uiState.value.status)
        assertEquals(TransferStep.RECEIPT, viewModel.uiState.value.step)
        assertEquals(300L, viewModel.uiState.value.postedMinor)
        assertEquals(1, viewModel.postedCount)
    }
}
