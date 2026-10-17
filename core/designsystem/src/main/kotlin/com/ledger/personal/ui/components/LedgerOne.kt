package com.ledger.personal.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
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

/** Full-width primary action. */
@Composable
fun LedgerOneButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth().height(52.dp),
    ) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.size(18.dp))
            Box(Modifier.size(8.dp))
        }
        LedgerOneText(text, role = LedgerOneTextRole.Label, weight = FontWeight.SemiBold)
    }
}

/** Single-line field on the Ledger One shape. */
@Composable
fun LedgerOneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    prefix: String? = null,
    supportingText: String? = null,
    isError: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it) } },
        singleLine = true,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Surface card used by sections. */
@Composable
fun LedgerOneCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@PreviewLightDark
@Composable
private fun LedgerOnePreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LedgerOneText("Ledger One", role = LedgerOneTextRole.Display)
            LedgerOneCard {
                LedgerOneText("Available balance", role = LedgerOneTextRole.Label)
                LedgerOneText("₹12,450.00", role = LedgerOneTextRole.Title)
                LedgerOneTextField("500", {}, label = "Amount", prefix = "₹")
                LedgerOneButton("Continue", onClick = {})
            }
        }
    }
}
