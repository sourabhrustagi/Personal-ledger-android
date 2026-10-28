package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.TransferBook
import com.ledger.personal.core.transactionFailureMessage
import com.ledger.personal.core.withRetry
import com.ledger.personal.mock.MockCatalog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State and actions for the wallet screen. The rail is chosen for the flavor. */
@HiltViewModel
class WalletViewModel @Inject constructor(
    private val rail: PaymentRail,
) : ViewModel() {
    private var retryDelayMs = 1_000L

    constructor(rail: PaymentRail, retryDelayMs: Long) : this(rail) {
        this.retryDelayMs = retryDelayMs
    }

    private val book = TransferBook()
    private val openingMinor = MockCatalog.transactions.filter { it.currency == "INR" }.sumOf { it.amountMinor }
    private var transfers = 1
    private var fsmState: PaymentFsmState = PaymentFsmState.Form(idempotencyKey = "transfer-$transfers")
    private val state = MutableStateFlow(WalletUiState(balanceMinor = openingMinor))
    val uiState: StateFlow<WalletUiState> = state.asStateFlow()

    /** Reacts to amount. */
    fun onAmount(minor: Long) {
        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.SetAmount(minor))
        state.update { it.copy(amountMinor = minor, status = TransferStatus.IDLE, errorMessage = null) }
    }

    /** Reacts to recipient. */
    fun onRecipient(handle: String) {
        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.SetRecipient(handle))
        state.update { it.copy(recipient = handle, errorMessage = null) }
    }

    /** Reacts to note. */
    fun onNote(note: String) {
        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.SetNote(note))
        state.update { it.copy(note = note) }
    }

    /** Review. */
    fun review() {
        val amount = state.value.amountMinor
        if (amount <= 0L) {
            state.update { it.copy(errorMessage = "Amount must be strictly positive") }
            return
        }
        if (amount > state.value.balanceMinor) {
            state.update { it.copy(status = TransferStatus.INSUFFICIENT, errorMessage = "Insufficient funds") }
            return
        }
        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.Review(state.value.balanceMinor))
        state.update { it.copy(step = TransferStep.REVIEW, status = TransferStatus.STEP_UP, errorMessage = null) }
    }

    /** Edit. */
    fun edit() {
        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.Edit)
        state.update { it.copy(step = TransferStep.FORM, status = TransferStatus.IDLE, errorMessage = null, canReconcile = false) }
    }

    /** Sends after a successful step-up. Dropped connections and a busy server are retried with the same reference. */
    fun onStepUp(success: Boolean) {
        val current = state.value
        val allowedStatus = current.status == TransferStatus.STEP_UP ||
            current.status == TransferStatus.FAILED ||
            current.status == TransferStatus.IN_DOUBT
        if (!success || !allowedStatus) return
        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.AuthSuccess(true))
        state.update { it.copy(status = TransferStatus.PENDING, attempt = 1, errorMessage = null, canReconcile = false) }
        val key = current.idempotencyKey
        val amount = current.amountMinor
        viewModelScope.launch {
            val result = withRetry(
                firstDelayMs = retryDelayMs,
                onAttempt = { attempt ->
                    fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.RetryAttempt(attempt))
                    state.update { it.copy(attempt = attempt) }
                },
            ) { rail { book.submit(key, amount) } }
            state.update { latest ->
                result.fold(
                    onSuccess = { posted ->
                        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.SubmitSuccess(posted))
                        latest.copy(
                            status = TransferStatus.DONE,
                            step = TransferStep.RECEIPT,
                            postedMinor = posted,
                            balanceMinor = openingMinor - book.total,
                            canReconcile = false,
                        )
                    },
                    onFailure = { error ->
                        val isTransient = error is java.io.IOException
                        val failureMsg = transactionFailureMessage(error)
                        fsmState = if (isTransient) {
                            PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.SubmitTransientFailure(failureMsg))
                        } else {
                            PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.SubmitTerminalFailure(failureMsg))
                        }
                        latest.copy(
                            status = if (isTransient) TransferStatus.IN_DOUBT else TransferStatus.FAILED,
                            balanceMinor = openingMinor - book.total,
                            errorMessage = failureMsg,
                            canReconcile = isTransient,
                        )
                    },
                )
            }
        }
    }

    /** Sends again with the same reference, so a payment that already landed is not charged twice. */
    fun retry() {
        if (state.value.status == TransferStatus.FAILED || state.value.status == TransferStatus.IN_DOUBT) onStepUp(true)
    }

    /**
     * Inquires banking status for unconfirmed/in-doubt transactions using the active idempotency key
     * to safely recover confirmed state without risking double charging.
     */
    fun reconcile() {
        val key = state.value.idempotencyKey
        viewModelScope.launch {
            val posted = book.status(key)
            fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.Reconciled(posted))
            state.update { latest ->
                if (posted != null) {
                    latest.copy(
                        status = TransferStatus.DONE,
                        step = TransferStep.RECEIPT,
                        postedMinor = posted,
                        balanceMinor = openingMinor - book.total,
                        canReconcile = false,
                        errorMessage = null,
                    )
                } else {
                    latest.copy(
                        status = TransferStatus.FAILED,
                        errorMessage = "The bank has no record of this payment. You may safely retry.",
                        canReconcile = false,
                    )
                }
            }
        }
    }

    /** New transfer. */
    fun newTransfer() {
        transfers += 1
        val nextKey = "transfer-$transfers"
        fsmState = PaymentFsmReducer.reduce(fsmState, PaymentFsmEvent.Reset(nextKey))
        state.update {
            WalletUiState(balanceMinor = it.balanceMinor, idempotencyKey = nextKey)
        }
    }

    /** Posted count. */
    val postedCount: Int get() = book.size

    /** Triggered when the user clicks a preset amount chip (e.g. ₹100, ₹500, ₹1,000). */
    fun onPresetAmountClicked(rupees: Long) {
        onAmount(rupees * 100)
    }

    /** Triggered when the user clicks a contact row or radio button in saved contacts. */
    fun onSelectContactClicked(handle: String) {
        onRecipient(handle)
    }

    /** Triggered when the user clicks the select from contacts action. */
    fun onPickContactClicked(launchPicker: () -> Unit) {
        launchPicker()
    }

    /** Triggered when the user clicks Continue to review the payment. */
    fun onContinueClicked() {
        review()
    }

    /** Triggered when the user clicks Check Status on unconfirmed / in-doubt payment. */
    fun onCheckStatusClicked() {
        reconcile()
    }

    /** Triggered when the user clicks Retry on failed / in-doubt payment. */
    fun onRetryClicked() {
        retry()
    }

    /** Triggered when the user clicks Edit details to return to the form step. */
    fun onEditDetailsClicked() {
        edit()
    }

    /** Triggered when the user clicks Done on the receipt screen. */
    fun onDoneClicked(onDone: () -> Unit) {
        onDone()
    }

    /** Triggered when the user clicks Send another payment on the receipt screen. */
    fun onSendAnotherClicked() {
        newTransfer()
    }

    /** Triggered when the user completes or triggers biometric authentication step-up. */
    fun onStepUpAuthClicked(success: Boolean) {
        onStepUp(success)
    }
}
