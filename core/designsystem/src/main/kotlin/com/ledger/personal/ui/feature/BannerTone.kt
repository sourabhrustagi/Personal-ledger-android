package com.ledger.personal.ui.feature

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ledger.personal.ui.theme.LedgerTheme

/** Allowed values of banner tone. */
enum class BannerTone { INFO, SUCCESS, WARNING, ERROR }

/** Tone color. */
@Composable
fun BannerTone.toneColor(): Color = when (this) {
    BannerTone.INFO -> MaterialTheme.colorScheme.secondary
    BannerTone.SUCCESS -> LedgerTheme.colors.success
    BannerTone.WARNING -> LedgerTheme.colors.warning
    BannerTone.ERROR -> MaterialTheme.colorScheme.error
}
