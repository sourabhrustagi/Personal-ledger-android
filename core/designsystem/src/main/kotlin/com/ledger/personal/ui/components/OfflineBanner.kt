package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows offline banner. */
@Composable
fun OfflineBanner(ageLabel: String?, modifier: Modifier = Modifier) {
    StatusBanner(
        message = ageLabel?.let { "You're offline. Showing the last saved copy from $it." } ?: "You're offline. Showing the last saved copy.",
        tone = BannerTone.WARNING,
        title = "No connection",
        modifier = modifier,
    )
}

/** Shows payments offline notice. */
@Composable
fun PaymentsOfflineNotice(modifier: Modifier = Modifier) {
    StatusBanner("Payments need a live, secure connection. Try again when you're back online.", BannerTone.WARNING, modifier, title = "Payments paused")
}

@PreviewLightDark
@Composable
private fun OfflineBannerPreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OfflineBanner("2:14 PM")
            PaymentsOfflineNotice()
        }
    }
}
