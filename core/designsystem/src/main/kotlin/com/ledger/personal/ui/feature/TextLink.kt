package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows text link. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.padding(0.dp)) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@PreviewLightDark
@Composable
private fun TextLinkPreview() {
    PersonalLedgerTheme {
        TextLink("Forgot MPIN?", onClick = {})
    }
}
