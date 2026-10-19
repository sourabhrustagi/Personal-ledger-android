package com.ledger.personal.ui.feature

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.LedgerTheme
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows amount text. */
@Composable
fun AmountText(
    minor: Long,
    direction: MoneyDirection,
    modifier: Modifier = Modifier,
    currency: String = "INR",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    label: String? = null,
) {
    val color = when (direction) {
        MoneyDirection.CREDIT -> LedgerTheme.colors.success
        MoneyDirection.DEBIT -> MaterialTheme.colorScheme.onSurface
        MoneyDirection.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    val kind = when (direction) {
        MoneyDirection.CREDIT -> "Income"
        MoneyDirection.DEBIT -> "Expense"
        MoneyDirection.NEUTRAL -> "Amount"
    }
    Text(
        formatSigned(minor, direction, currency),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = listOfNotNull(kind, label, spokenAmount(minor, currency)).joinToString(", ")
        },
    )
}

@PreviewLightDark
@Composable
private fun AmountTextPreview() {
    PersonalLedgerTheme {
        AmountText(45_500L, MoneyDirection.DEBIT, label = "Team lunch")
    }
}
