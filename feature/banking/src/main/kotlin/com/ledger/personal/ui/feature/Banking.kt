package com.ledger.personal.ui.feature

import com.ledger.personal.core.EmiLeg
import com.ledger.personal.core.amortize
import com.ledger.personal.core.emiMinor
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.SeedAccount

/** Values rendered by the banking screen. */
data class BankingUiState(
    val isLoading: Boolean = false,
    val accounts: List<String> = MockCatalog.accounts,
    val bankAccounts: List<SeedAccount> = MockCatalog.bankAccounts,
    val emiMinor: Long = emiMinor(MockCatalog.loanPrincipalMinor, LOAN_BPS, LOAN_MONTHS),
    val legs: List<EmiLeg> = amortize(MockCatalog.loanPrincipalMinor, LOAN_BPS, LOAN_MONTHS),
    val upi: String? = null,
    val payee: String? = null,
    val signedIn: Boolean = false,
    val cooling: Boolean = false,
    val isEmpty: Boolean = false,
    val errorMessage: String? = null,
)

const val LOAN_BPS = 1_200
const val LOAN_MONTHS = 36
