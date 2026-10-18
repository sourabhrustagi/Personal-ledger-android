package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.PayBox
import com.ledger.personal.domain.pay.BillPay
import com.ledger.personal.domain.pay.SealedPay
import com.ledger.personal.core.mpinAccepted
import com.ledger.personal.feature.pos.billReceipt
import com.ledger.personal.mock.SeedBiller
import com.ledger.personal.mock.SeedCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** State and actions for bill payment. */
@HiltViewModel
class PosViewModel @Inject constructor(private val rail: BillPay) : ViewModel() {
    private val state = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = state.asStateFlow()

    /** Electricity asks for a bill number. Other bills go straight to the card. */
    fun selectBill(biller: SeedBiller) = state.update {
        val electricity = biller.id == "electricity"
        it.copy(
            biller = biller,
            card = null,
            billNumber = "",
            fetched = null,
            fetchFailed = false,
            step = if (electricity) PosStep.ACCOUNT else PosStep.CARD,
            errorMessage = null,
        )
    }

    /** Digits from the electricity bill. Clears a previous lookup. */
    fun onBillNumber(raw: String) = state.update {
        it.copy(
            billNumber = raw.filter(Char::isDigit).take(12),
            fetched = null,
            fetchFailed = false,
            errorMessage = null,
        )
    }

    /** Seals the bill number, then asks the board. A miss stays on this step. */
    fun fetchBill() {
        val biller = state.value.biller ?: return
        val number = state.value.billNumber
        if (number.length < 6) {
            state.update { it.copy(fetchFailed = false, errorMessage = "Enter the bill number from your electricity bill.") }
            return
        }
        viewModelScope.launch {
            state.update { it.copy(fetching = true, fetchFailed = false, errorMessage = null) }
            val result = runCatching {
                rail.fetch(seal(PayBox.packLookup(biller.id, number))).getOrThrow()
            }.onFailure { if (it is CancellationException) throw it }
            state.update { current ->
                result.fold(
                    onSuccess = { bill -> current.copy(fetching = false, fetched = bill, fetchFailed = false) },
                    onFailure = {
                        current.copy(
                            fetching = false,
                            fetched = null,
                            fetchFailed = true,
                            errorMessage = "The electricity board did not return a bill for this number. " +
                                "Check the bill number and try again. Your card was not charged.",
                        )
                    },
                )
            }
        }
    }

    /** Card step, after a bill is on screen. */
    fun continueToPay() {
        if (state.value.fetched == null) return
        state.update { it.copy(step = PosStep.CARD, card = null, errorMessage = null) }
    }

    /** Picks the card, then the PIN. */
    fun selectCard(card: SeedCard) = state.update {
        it.copy(card = card, step = PosStep.PIN, errorMessage = null)
    }

    /** Back to the previous step. */
    fun back() = state.update {
        when (it.step) {
            PosStep.PIN -> it.copy(step = PosStep.CARD, errorMessage = null)
            PosStep.CARD -> if (it.biller?.id == "electricity") {
                it.copy(step = PosStep.ACCOUNT, card = null, errorMessage = null)
            } else {
                it.copy(step = PosStep.BILLS, biller = null, errorMessage = null)
            }
            PosStep.ACCOUNT -> it.copy(step = PosStep.BILLS, biller = null, fetched = null, fetchFailed = false, errorMessage = null)
            PosStep.BILLS -> it
        }
    }

    /** Seals card, bill, and PIN with the agreed key, then submits. */
    fun confirmPin(pin: String) {
        val current = state.value
        val biller = current.biller ?: return
        val card = current.card ?: return
        if (!mpinAccepted(pin)) {
            state.update { it.copy(errorMessage = "Enter a 4-digit PIN") }
            return
        }
        val amount = current.fetched?.amountMinor ?: biller.dueMinor
        val billNumber = current.fetched?.billNumber.orEmpty()
        viewModelScope.launch {
            state.update { it.copy(paying = true, errorMessage = null) }
            val result = runCatching {
                rail.submit(seal(PayBox.pack(card.id, biller.id, amount, pin, billNumber))).getOrThrow()
            }.onFailure { if (it is CancellationException) throw it }
            state.update {
                if (result.isSuccess) {
                    it.copy(
                        paying = false,
                        step = PosStep.BILLS,
                        bill = billReceipt(biller.id, amount),
                        paidBillers = it.paidBillers + biller.id,
                        biller = null,
                        card = null,
                        fetched = null,
                        billNumber = "",
                    )
                } else {
                    it.copy(paying = false, errorMessage = "Couldn't pay. Try again.")
                }
            }
        }
    }

    private suspend fun seal(plain: ByteArray): SealedPay {
        val mine = PayBox.keyPair()
        val secret = PayBox.agree(mine.private, rail.serverPublic())
        val packed = PayBox.seal(secret, plain)
        return SealedPay(mine.public.encoded, packed.iv, packed.body)
    }
}
