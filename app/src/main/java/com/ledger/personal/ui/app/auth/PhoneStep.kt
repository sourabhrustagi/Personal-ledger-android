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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.session.OtpResponse
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.launch

private const val PHONE_LENGTH = 10

/** Shows phone number input step. */
@Composable
fun PhoneStep(
    phone: String,
    onPhone: (String) -> Unit,
    onSendCode: suspend (phone: String) -> Result<OtpResponse> = { Result.success(OtpResponse()) },
    onNext: (response: OtpResponse) -> Unit,
) {
    var consent by rememberSaveable { mutableStateOf(false) }
    var isSending by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Text("Enter your mobile number", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
        "We'll send a 6-digit verification code to verify your phone.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (errorMessage != null) {
        StatusBanner(errorMessage ?: "Failed to send code. Please try again.", BannerTone.ERROR)
    }
    OutlinedTextField(
        value = phone,
        onValueChange = { next ->
            if (next.length <= PHONE_LENGTH && next.all(Char::isDigit)) {
                onPhone(next)
                errorMessage = null
            }
        },
        label = { Text("Mobile number") },
        prefix = { Text("+91 ") },
        supportingText = { Text("10-digit mobile number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        enabled = !isSending,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !isSending)
        Text("I agree to the Terms of Service and Privacy Policy.", style = MaterialTheme.typography.bodyMedium)
    }
    PrimaryButton(
        text = if (isSending) "Sending code..." else "Send code",
        onClick = {
            if (phone.length == PHONE_LENGTH && phone.all(Char::isDigit)) {
                isSending = true
                errorMessage = null
                scope.launch {
                    val result = onSendCode(phone)
                    isSending = false
                    result.fold(
                        onSuccess = { res ->
                            onNext(res)
                        },
                        onFailure = { error ->
                            errorMessage = error.message ?: "Failed to send OTP. Please try again."
                        },
                    )
                }
            } else {
                errorMessage = "Please enter a valid 10-digit mobile number."
            }
        },
        enabled = consent && phone.length == PHONE_LENGTH && !isSending,
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
                PhoneStep(MockValues.DEFAULT_PHONE, {}, onSendCode = { Result.success(OtpResponse()) }) {}
            }
        }
    }
}
