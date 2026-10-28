package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.components.LedgerOneTextField
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows amount field. */
@Composable
fun AmountField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    symbol: String = "₹",
    supportingText: String? = null,
    isError: Boolean = false,
) {
    LedgerOneTextField(
        value = text,
        onValueChange = { value -> if (amountInput.matches(value)) onTextChange(value) },
        modifier = modifier,
        label = label,
        prefix = symbol,
        supportingText = supportingText,
        isError = isError,
        keyboardType = KeyboardType.Decimal,
    )
}

private val amountInput = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

@PreviewLightDark
@Composable
private fun AmountFieldPreview() {
    PersonalLedgerTheme { AmountField("500", {}, Modifier.padding(16.dp)) }
}
