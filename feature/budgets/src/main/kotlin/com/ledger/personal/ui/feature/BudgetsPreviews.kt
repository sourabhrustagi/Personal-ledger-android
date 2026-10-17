package com.ledger.personal.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

@PreviewLightDark
@Composable
private fun BudgetsPreview() {
    PersonalLedgerTheme { BudgetsScreen(onBack = {}, onInsights = {}) }
}

@PreviewLightDark
@Composable
private fun ChartsPreview() {
    PersonalLedgerTheme { ChartsScreen(onBack = {}, viewModel = ChartsViewModel()) }
}
