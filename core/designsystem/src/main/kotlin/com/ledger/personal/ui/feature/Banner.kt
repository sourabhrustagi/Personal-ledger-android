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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.LedgerTheme
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Allowed values of banner tone. */
enum class BannerTone { INFO, SUCCESS, WARNING, ERROR }

/** Tone color. */
@Composable
fun toneColor(tone: BannerTone): Color = when (tone) {
    BannerTone.INFO -> MaterialTheme.colorScheme.secondary
    BannerTone.SUCCESS -> LedgerTheme.colors.success
    BannerTone.WARNING -> LedgerTheme.colors.warning
    BannerTone.ERROR -> MaterialTheme.colorScheme.error
}

/** Shows status banner. */
@Composable
fun StatusBanner(message: String, tone: BannerTone, modifier: Modifier = Modifier, title: String? = null) {
    val color = toneColor(tone)
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

/** Shows status chip. */
@Composable
fun StatusChip(text: String, tone: BannerTone, modifier: Modifier = Modifier) {
    val color = toneColor(tone)
    Surface(modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun BannerPreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusBanner("You're offline. Showing the last saved copy.", BannerTone.WARNING, title = "No connection")
            StatusChip("Triggered", BannerTone.SUCCESS)
        }
    }
}
