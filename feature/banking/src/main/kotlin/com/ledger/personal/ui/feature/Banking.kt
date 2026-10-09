package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import com.ledger.personal.core.EmiLeg
import com.ledger.personal.core.amortize
import com.ledger.personal.core.coolingBlocks
import com.ledger.personal.core.emiMinor
import com.ledger.personal.core.mpinAccepted
import com.ledger.personal.core.upiPayload
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.SeedAccount
import com.ledger.personal.mock.SeedBiller
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BankingUiState(
    val isLoading: Boolean = false,
    val accounts: List<String> = MockCatalog.accounts,
    val bankAccounts: List<SeedAccount> = MockCatalog.bankAccounts,
    val billers: List<SeedBiller> = MockCatalog.billers,
    val paidBillers: Set<String> = emptySet(),
    val emiMinor: Long = emiMinor(MockCatalog.loanPrincipalMinor, LOAN_BPS, LOAN_MONTHS),
    val legs: List<EmiLeg> = amortize(MockCatalog.loanPrincipalMinor, LOAN_BPS, LOAN_MONTHS),
    val upi: String? = null,
    val bill: String? = null,
    val payee: String? = null,
    val signedIn: Boolean = false,
    val cooling: Boolean = false,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
)

const val LOAN_BPS = 1_200
const val LOAN_MONTHS = 36

class BankingViewModel : ViewModel() {
    private val state = MutableStateFlow(BankingUiState())
    val uiState: StateFlow<BankingUiState> = state.asStateFlow()
    private var beneficiaryAddedAt: Long? = null

    fun signIn(pin: String) {
        state.update { it.copy(signedIn = mpinAccepted(pin), errorMessage = if (mpinAccepted(pin)) null else "MPIN rejected") }
    }

    fun signOut() = state.update { BankingUiState() }

    fun showUpi(vpa: String, amountMinor: Long) {
        state.update { it.copy(upi = upiPayload(vpa, amountMinor)) }
    }

    fun addBeneficiary(nowEpochMs: Long, name: String = "New payee") {
        beneficiaryAddedAt = nowEpochMs
        state.update { it.copy(payee = name, cooling = false, errorMessage = null) }
    }

    fun pay(nowEpochMs: Long) {
        val added = beneficiaryAddedAt
        val blocked = added != null && coolingBlocks(added, nowEpochMs)
        state.update { it.copy(cooling = blocked, errorMessage = if (blocked) "Beneficiary is cooling" else null) }
    }

    fun payBill(biller: String, amountMinor: Long) {
        if (!state.value.signedIn) {
            state.update { it.copy(errorMessage = "Sign in required") }
            return
        }
        state.update { it.copy(bill = "$biller:$amountMinor", paidBillers = it.paidBillers + biller, errorMessage = null) }
    }

    fun dismissMessage() = state.update { it.copy(errorMessage = null, cooling = false) }
}
