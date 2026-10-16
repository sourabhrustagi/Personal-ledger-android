package com.ledger.personal.ui.feature

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.SavedStateHandle
import com.ledger.personal.ui.theme.PersonalLedgerTheme

@PreviewLightDark
@Composable
private fun KycPreview() {
    PersonalLedgerTheme { KycScreen(onBack = {}, liveCapture = false, viewModel = KycViewModel(SavedStateHandle())) }
}
