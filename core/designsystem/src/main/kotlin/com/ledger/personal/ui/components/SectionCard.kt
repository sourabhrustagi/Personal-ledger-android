package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.components.LedgerOneCard
import com.ledger.personal.ui.components.LedgerOneText
import com.ledger.personal.ui.components.LedgerOneTextRole
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows section card. */
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

/** Shows section header. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

@PreviewLightDark
@Composable
private fun SectionCardPreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Bills")
            SectionCard(title = "This month") { Text("Rent") }
        }
    }
}
