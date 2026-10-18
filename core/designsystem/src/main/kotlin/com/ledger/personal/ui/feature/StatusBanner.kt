package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows status banner. */
@Composable
fun StatusBanner(message: String, tone: BannerTone, modifier: Modifier = Modifier, title: String? = null) {
    val color = tone.toneColor()
    val icon = when (tone) {
        BannerTone.INFO -> Icons.Outlined.Info
        BannerTone.SUCCESS -> Icons.Outlined.CheckCircle
        BannerTone.WARNING -> Icons.Outlined.WarningAmber
        BannerTone.ERROR -> Icons.Outlined.ErrorOutline
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                title?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StatusBannerPreview() {
    PersonalLedgerTheme {
        StatusBanner(
            "You're offline. Showing the last saved copy.",
            BannerTone.WARNING,
            Modifier.padding(16.dp),
            title = "No connection",
        )
    }
}
