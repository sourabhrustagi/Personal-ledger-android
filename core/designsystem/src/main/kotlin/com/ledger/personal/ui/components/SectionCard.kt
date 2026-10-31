package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/**
 * Standard card container used to visually group related form fields, account details, or summary rows.
 * Responsible solely for container elevation, borders, padding, and optional section title.
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LedgerOneCard(modifier) {
        title?.let { LedgerOneText(it, role = LedgerOneTextRole.Title, weight = FontWeight.SemiBold) }
        content()
    }
}

@PreviewLightDark
@Composable
private fun SectionCardPreview() {
    PersonalLedgerTheme {
        SectionCard(title = "App Protection") {
            DetailRow("Biometric Lock", "Enabled")
            DetailRow("Auto-Lock Timer", "Immediately")
        }
    }
}
