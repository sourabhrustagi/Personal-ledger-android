package com.ledger.personal.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

@PreviewLightDark
@Composable
private fun BankingLockPreview() {
    PersonalLedgerTheme { BankingScreen(onBack = {}, onPayBills = {}, viewModel = BankingViewModel()) }
}

@PreviewLightDark
@Composable
private fun BankingPreview() {
    val signedIn = remember { BankingViewModel().also { it.signIn("1234") } }
    PersonalLedgerTheme { BankingScreen(onBack = {}, onPayBills = {}, viewModel = signedIn) }
}

@PreviewLightDark
@Composable
private fun CardsPreview() {
    PersonalLedgerTheme { CardsScreen(onBack = {}) }
}

@PreviewLightDark
@Composable
private fun LoansPreview() {
    PersonalLedgerTheme { LoansScreen(onBack = {}) }
}

@PreviewLightDark
@Composable
private fun DepositsPreview() {
    PersonalLedgerTheme { DepositsScreen(onBack = {}) }
}
