package com.ledger.personal.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Which Ledger One text style to draw. */
enum class LedgerOneTextRole { Display, Title, Body, Label }

/** Themed text. Inter and tabular figures come from the theme. */
@Composable
fun LedgerOneText(
    text: String,
    modifier: Modifier = Modifier,
    role: LedgerOneTextRole = LedgerOneTextRole.Body,
    color: Color = Color.Unspecified,
    weight: FontWeight? = null,
    align: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    val type = MaterialTheme.typography
    val base = when (role) {
        LedgerOneTextRole.Display -> type.headlineMedium
        LedgerOneTextRole.Title -> type.titleMedium
        LedgerOneTextRole.Body -> type.bodyMedium
        LedgerOneTextRole.Label -> type.labelLarge
    }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        textAlign = align,
        maxLines = maxLines,
        overflow = if (maxLines == Int.MAX_VALUE) TextOverflow.Clip else TextOverflow.Ellipsis,
        style = if (weight == null) base else base.copy(fontWeight = weight),
    )
}

@PreviewLightDark
@Composable
private fun LedgerOneTextPreview() {
    PersonalLedgerTheme {
        LedgerOneText("Ledger One", role = LedgerOneTextRole.Display)
    }
}
