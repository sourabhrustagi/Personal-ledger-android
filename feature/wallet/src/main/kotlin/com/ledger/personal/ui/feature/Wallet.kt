package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import com.ledger.personal.core.TransferBook
import com.ledger.personal.mock.MockCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TransferStep { FORM, REVIEW, RECEIPT }

enum class TransferStatus { IDLE, INSUFFICIENT, STEP_UP, PENDING, DONE }

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
    val errorMessage: String? = null,
)

class WalletViewModel : ViewModel() {
    private val book = TransferBook()
    private val state = MutableStateFlow(
        WalletUiState(balanceMinor = MockCatalog.transactions.filter { it.currency == "INR" }.sumOf { it.amountMinor }),
    )
    val uiState: StateFlow<WalletUiState> = state.asStateFlow()
    private var transfers = 1

    fun onAmount(minor: Long) = state.update { it.copy(amountMinor = minor, status = TransferStatus.IDLE, errorMessage = null) }

    fun onRecipient(handle: String) = state.update { it.copy(recipient = handle, errorMessage = null) }

    fun onNote(note: String) = state.update { it.copy(note = note) }

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

    fun edit() = state.update { it.copy(step = TransferStep.FORM, status = TransferStatus.IDLE) }

    fun onStepUp(success: Boolean) {
        if (!success || state.value.status != TransferStatus.STEP_UP && state.value.status != TransferStatus.PENDING) return
        state.update { it.copy(status = TransferStatus.PENDING) }
        val before = book.size()
        val posted = book.submit(state.value.idempotencyKey, state.value.amountMinor)
        val debited = if (book.size() > before) posted else 0L
        state.update {
            it.copy(
                status = TransferStatus.DONE,
                step = TransferStep.RECEIPT,
                postedMinor = posted,
                balanceMinor = it.balanceMinor - debited,
            )
        }
    }

    fun retry() {
        state.update { it.copy(status = TransferStatus.STEP_UP, step = TransferStep.REVIEW) }
        onStepUp(true)
    }

    fun newTransfer() {
        transfers += 1
        state.update {
            WalletUiState(balanceMinor = it.balanceMinor, idempotencyKey = "transfer-$transfers")
        }
    }

    fun postedCount(): Int = book.size()
}
