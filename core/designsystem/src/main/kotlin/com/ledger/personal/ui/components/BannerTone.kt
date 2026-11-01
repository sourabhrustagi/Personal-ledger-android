package com.ledger.personal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ledger.personal.ui.theme.LedgerTheme
import com.ledger.personal.ui.theme.colorScheme

/** Allowed values of banner tone. */
enum class BannerTone { INFO, SUCCESS, WARNING, ERROR }

/** Tone color. */
@Composable
fun BannerTone.toneColor(): Color = when (this) {
    BannerTone.INFO -> colorScheme.secondary
    BannerTone.SUCCESS -> LedgerTheme.colors.success
    BannerTone.WARNING -> LedgerTheme.colors.warning
    BannerTone.ERROR -> colorScheme.error
}
