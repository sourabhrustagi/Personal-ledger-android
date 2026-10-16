package com.ledger.personal.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

@PreviewLightDark
@Composable
private fun TickerPreview() {
    PersonalLedgerTheme { TickerScreen(onBack = {}, viewModel = TickerViewModel()) }
}
