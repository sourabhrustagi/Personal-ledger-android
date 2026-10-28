package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.components.LedgerOneButton
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows primary button. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    LedgerOneButton(text, onClick, modifier, enabled, icon)
}

@PreviewLightDark
@Composable
private fun PrimaryButtonPreview() {
    PersonalLedgerTheme { PrimaryButton("Continue", onClick = {}, modifier = Modifier.padding(16.dp)) }
}
