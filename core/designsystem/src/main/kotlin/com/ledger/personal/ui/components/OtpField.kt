package com.ledger.personal.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows otp field. */
@Composable
fun OtpField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, length: Int = 6) {
    BasicTextField(
        value = value,
        onValueChange = { next -> if (next.length <= length && next.all { it.isDigit() }) onValueChange(next) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier.semantics { contentDescription = "One-time code, $length digits" },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)
                    val focused = index == value.length
                    Box(
                        Modifier.size(width = 44.dp, height = 56.dp)
                            .border(
                                if (focused) 2.dp else 1.dp,
                                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(char?.toString() ?: "", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun OtpFieldPreview() {
    PersonalLedgerTheme {
        OtpField("12", {})
    }
}
