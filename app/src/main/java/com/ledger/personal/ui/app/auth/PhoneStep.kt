package com.ledger.personal.ui.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.theme.PersonalLedgerTheme

private const val PHONE_LENGTH = 10

/** Shows phone number input step. */
@Composable
fun PhoneStep(
    phone: String,
    onPhone: (String) -> Unit,
    onNext: () -> Unit,
) {
    var consent by rememberSaveable { mutableStateOf(false) }
    Text("Enter your mobile number", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
        "We'll send a 6-digit verification code to verify your phone.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        phone,
        { next -> if (next.length <= PHONE_LENGTH && next.all(Char::isDigit)) onPhone(next) },
        label = { Text("Mobile number") },
        prefix = { Text("+91 ") },
        supportingText = { Text("10-digit mobile number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(consent, { consent = it })
        Text("I agree to the Terms of Service and Privacy Policy.", style = MaterialTheme.typography.bodyMedium)
    }
    PrimaryButton(
        "Send code",
        onClick = onNext,
        enabled = consent && phone.length == PHONE_LENGTH,
        icon = Icons.Outlined.Sms,
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewLightDark
@Composable
private fun PhoneStepPreview() {
    PersonalLedgerTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PhoneStep(MockValues.DEFAULT_PHONE, {}) {}
            }
        }
    }
}
