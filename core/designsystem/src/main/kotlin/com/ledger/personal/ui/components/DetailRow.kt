package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.theme.colorScheme
import com.ledger.personal.ui.theme.typography

/** Shows detail row. */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = typography.bodyMedium, color = colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (emphasize) typography.titleMedium else typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@PreviewLightDark
@Composable
private fun DetailRowPreview() {
    PersonalLedgerTheme { DetailRow("Reference", "transfer-1", Modifier.padding(16.dp)) }
}
