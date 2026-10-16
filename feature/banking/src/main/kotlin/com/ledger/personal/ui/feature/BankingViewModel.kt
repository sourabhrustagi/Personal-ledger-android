package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import com.ledger.personal.core.coolingBlocks
import com.ledger.personal.core.mpinAccepted
import com.ledger.personal.core.upiPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** State and actions for the banking screen. */
class BankingViewModel : ViewModel() {
    private val state = MutableStateFlow(BankingUiState())
    val uiState: StateFlow<BankingUiState> = state.asStateFlow()
    private var beneficiaryAddedAt: Long? = null

    /** Sign in. */
    fun signIn(pin: String) {
        state.update { it.copy(signedIn = mpinAccepted(pin), errorMessage = if (mpinAccepted(pin)) null else "MPIN rejected") }
    }

    /** Sign out. */
    fun signOut() = state.update { BankingUiState() }

    /** Shows upi. */
    fun showUpi(vpa: String, amountMinor: Long) {
        state.update { it.copy(upi = upiPayload(vpa, amountMinor)) }
    }

    /** Add beneficiary. */
    fun addBeneficiary(nowEpochMs: Long, name: String = "New payee") {
        beneficiaryAddedAt = nowEpochMs
        state.update { it.copy(payee = name, cooling = false, errorMessage = null) }
    }

    /** Pay. */
    fun pay(nowEpochMs: Long) {
        val added = beneficiaryAddedAt
        val blocked = added != null && coolingBlocks(added, nowEpochMs)
        state.update { it.copy(cooling = blocked, errorMessage = if (blocked) "Beneficiary is cooling" else null) }
    }

    /** Dismiss message. */
    fun dismissMessage() = state.update { it.copy(errorMessage = null, cooling = false) }
}
