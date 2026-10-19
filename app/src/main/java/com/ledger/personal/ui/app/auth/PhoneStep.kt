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

/** Shows phone and name input step. */
@Composable
fun PhoneStep(
    name: String,
    phone: String,
    onName: (String) -> Unit,
    onPhone: (String) -> Unit,
    onNext: () -> Unit,
) {
    var consent by rememberSaveable { mutableStateOf(false) }
    Text("Let's get you set up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    OutlinedTextField(name, onName, label = { Text("Full name (as on PAN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        phone,
        { next -> if (next.length <= PHONE_LENGTH && next.all(Char::isDigit)) onPhone(next) },
        label = { Text("Mobile number") },
        prefix = { Text("+91 ") },
        supportingText = { Text("We'll send a one-time code by SMS") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(consent, { consent = it })
        Text("I agree to the Terms and Privacy Policy and allow device binding.", style = MaterialTheme.typography.bodyMedium)
    }
    PrimaryButton(
        "Send code",
        onClick = onNext,
        enabled = consent && phone.length == PHONE_LENGTH && name.isNotBlank(),
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
                PhoneStep(MockValues.DEFAULT_NAME, MockValues.DEFAULT_PHONE, {}, {}) {}
            }
        }
    }
}
