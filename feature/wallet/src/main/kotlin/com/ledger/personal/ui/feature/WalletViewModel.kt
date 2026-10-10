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
    private val state = MutableStateFlow(WalletUiState(balanceMinor = openingMinor))
    val uiState: StateFlow<WalletUiState> = state.asStateFlow()
    private var transfers = 1

    /** Reacts to amount. */
    fun onAmount(minor: Long) = state.update { it.copy(amountMinor = minor, status = TransferStatus.IDLE, errorMessage = null) }

    /** Reacts to recipient. */
    fun onRecipient(handle: String) = state.update { it.copy(recipient = handle, errorMessage = null) }

    /** Reacts to note. */
    fun onNote(note: String) = state.update { it.copy(note = note) }

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
        state.update { it.copy(step = TransferStep.REVIEW, status = TransferStatus.STEP_UP, errorMessage = null) }
    }

    /** Edit. */
    fun edit() = state.update { it.copy(step = TransferStep.FORM, status = TransferStatus.IDLE, errorMessage = null) }

    /** Sends after a successful step-up. Dropped connections and a busy server are retried with the same reference. */
    fun onStepUp(success: Boolean) {
        val current = state.value
        if (!success || current.status != TransferStatus.STEP_UP && current.status != TransferStatus.FAILED) return
        state.update { it.copy(status = TransferStatus.PENDING, attempt = 1, errorMessage = null) }
        val key = current.idempotencyKey
        val amount = current.amountMinor
        viewModelScope.launch {
            val result = withRetry(
                firstDelayMs = retryDelayMs,
                onAttempt = { attempt -> state.update { it.copy(attempt = attempt) } },
            ) { rail { book.submit(key, amount) } }
            state.update { latest ->
                result.fold(
                    onSuccess = { posted ->
                        latest.copy(
                            status = TransferStatus.DONE,
                            step = TransferStep.RECEIPT,
                            postedMinor = posted,
                            balanceMinor = openingMinor - book.total(),
                        )
                    },
                    onFailure = { error ->
                        latest.copy(
                            status = TransferStatus.FAILED,
                            balanceMinor = openingMinor - book.total(),
                            errorMessage = transactionFailureMessage(error),
                        )
                    },
                )
            }
        }
    }

    /** Sends again with the same reference, so a payment that already landed is not charged twice. */
    fun retry() {
        if (state.value.status == TransferStatus.FAILED) onStepUp(true)
    }

    /** New transfer. */
    fun newTransfer() {
        transfers += 1
        state.update {
            WalletUiState(balanceMinor = it.balanceMinor, idempotencyKey = "transfer-$transfers")
        }
    }

    /** Posted count. */
    fun postedCount(): Int = book.size()
}
