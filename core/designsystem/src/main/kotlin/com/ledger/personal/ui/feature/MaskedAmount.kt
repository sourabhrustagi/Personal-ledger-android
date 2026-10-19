package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows masked amount. */
@Composable
fun MaskedAmount(
    minor: Long,
    revealed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    currency: String = "INR",
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (revealed) formatMoney(minor, currency) else "₹ • • • • •",
            style = style,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.semantics {
                contentDescription = if (revealed) spokenAmount(minor, currency) else "Balance hidden"
            },
        )
        IconButton(onClick = onToggle) {
            Icon(
                if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (revealed) "Hide balance" else "Show balance",
                tint = color,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun MaskedAmountPreview() {
    PersonalLedgerTheme {
        MaskedAmount(2_45_678_90L, revealed = false, onToggle = {})
    }
}
