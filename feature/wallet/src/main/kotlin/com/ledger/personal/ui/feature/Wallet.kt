package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.TransferBook
import com.ledger.personal.core.transactionFailureMessage
import com.ledger.personal.core.withRetry
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.MockSwitches
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Allowed values of transfer step. */
enum class TransferStep { FORM, REVIEW, RECEIPT }

/** Allowed values of transfer status. */
enum class TransferStatus { IDLE, INSUFFICIENT, STEP_UP, PENDING, FAILED, DONE }

/** Values rendered by the wallet screen. */
data class WalletUiState(
    val isLoading: Boolean = false,
    val balanceMinor: Long = 0L,
    val amountMinor: Long = 0L,
    val recipient: String = "",
    val note: String = "",
    val step: TransferStep = TransferStep.FORM,
    val status: TransferStatus = TransferStatus.IDLE,
    val idempotencyKey: String = "transfer-1",
    val postedMinor: Long = 0L,
    val attempt: Int = 0,
    val errorMessage: String? = null,
)

/** Sends one payment. [post] records it on the ledger and must be called at most once per accepted attempt. */
typealias PaymentRail = suspend (post: () -> Long) -> Long

/** State and actions for the wallet screen. */
class WalletViewModel(
    private val rail: PaymentRail = { post -> MockSwitches.payment(post = post) },
    private val retryDelayMs: Long = 1_000,
) : ViewModel() {
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
