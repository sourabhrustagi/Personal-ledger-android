package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.PayEvent
import com.ledger.personal.core.PayState
import com.ledger.personal.core.next
import com.ledger.personal.data.local.CardReader
import com.ledger.personal.data.local.MockCardReader
import com.ledger.personal.data.local.MockReceiptPrinter
import com.ledger.personal.data.local.MemoryPosForward
import com.ledger.personal.data.local.PosForwardStore
import com.ledger.personal.data.local.ReceiptPrinter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Values rendered by the pos screen. */
data class PosUiState(
    val isLoading: Boolean = false,
    val pay: PayState = PayState.IDLE,
    val amountMinor: Long = 0L,
    val sale: Int = 1,
    val lastFour: String? = null,
    val forwarded: Int = 0,
    val pendingCount: Int = 0,
    val pendingTotalMinor: Long = 0L,
    val refunded: Boolean = false,
    val settlement: String? = null,
    val errorMessage: String? = null,
) {
    val reference: String get() = "pos-sale-$sale"
}

/** State and actions for the pos screen. */
@HiltViewModel
class PosViewModel @Inject constructor(
    private val forward: PosForwardStore,
    private val reader: CardReader,
    private val printer: ReceiptPrinter,
) : ViewModel() {
    constructor() : this(MemoryPosForward(), MockCardReader(), MockReceiptPrinter())

    private val state = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = state.asStateFlow()

    /** Reacts to digit. */
    fun onDigit(digit: Char) {
        if (state.value.pay != PayState.IDLE) return
        state.update {
            val next = it.amountMinor * 10 + digit.digitToInt()
            if (next > MAX_MINOR) it else it.copy(amountMinor = next, errorMessage = null)
        }
    }

    /** Reacts to backspace. */
    fun onBackspace() {
        if (state.value.pay == PayState.IDLE) state.update { it.copy(amountMinor = it.amountMinor / 10) }
    }

    /** Reacts to event. */
    fun onEvent(event: PayEvent) {
        runCatching { state.value.pay.next(event) }
            .onSuccess { next ->
                state.update { it.copy(pay = next, errorMessage = null) }
                if (next == PayState.READ) {
                    viewModelScope.launch { state.update { it.copy(lastFour = reader.readLastFour()) } }
                }
                if (next == PayState.SETTLED) {
                    viewModelScope.launch {
                        val current = state.value
                        forward.enqueue(current.reference, current.amountMinor)
                        printer.print("${current.reference} ${current.amountMinor}")
                        state.update { it.copy(forwarded = forward.count()) }
                        refreshPending()
                    }
                }
            }
            .onFailure { error -> state.update { it.copy(errorMessage = error.message) } }
    }

    /** New sale. */
    fun newSale() = state.update {
        PosUiState(sale = it.sale + 1, forwarded = it.forwarded, pendingCount = it.pendingCount, pendingTotalMinor = it.pendingTotalMinor)
    }

    /** Refund. */
    fun refund() {
        val current = state.value
        if (current.pay != PayState.SETTLED || current.refunded) return
        viewModelScope.launch {
            forward.enqueue("refund-${current.reference}", -current.amountMinor)
            printer.print("refund-${current.reference} -${current.amountMinor}")
            state.update { it.copy(refunded = true, forwarded = forward.count()) }
            refreshPending()
        }
    }

    /** Settle. */
    fun settle() {
        viewModelScope.launch {
            val total = forward.unsettled().sum()
            val closed = forward.settleAll()
            state.update { it.copy(settlement = if (closed == 0) "Nothing to settle" else "$closed entries settled · net ${formatMoney(total)}") }
            refreshPending()
        }
    }

    /** Reloads pending. */
    fun refreshPending() {
        viewModelScope.launch {
            val open = forward.unsettled()
            state.update { it.copy(pendingCount = open.size, pendingTotalMinor = open.sum()) }
        }
    }

    private companion object {
        const val MAX_MINOR = 10_000_000L
    }
}
