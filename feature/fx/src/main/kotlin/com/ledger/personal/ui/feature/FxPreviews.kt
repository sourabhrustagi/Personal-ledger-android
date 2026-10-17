package com.ledger.personal.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

@PreviewLightDark
@Composable
private fun FxAlertsPreview() {
    PersonalLedgerTheme { FxAlertsScreen(onBack = {}, viewModel = FxAlertsViewModel()) }
}
