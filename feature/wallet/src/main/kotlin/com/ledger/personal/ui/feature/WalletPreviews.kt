package com.ledger.personal.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

@PreviewLightDark
@Composable
private fun WalletPreview() {
    PersonalLedgerTheme { WalletScreen(onBack = {}, viewModel = WalletViewModel(PaymentRail { 0L })) }
}

@PreviewLightDark
@Composable
private fun SplitBillPreview() {
    PersonalLedgerTheme { SplitBillScreen(onBack = {}, initialMinor = 84_000L, note = "Dinner") }
}
