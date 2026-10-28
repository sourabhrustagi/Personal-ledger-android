@file:Suppress("MatchingDeclarationName")

package com.ledger.personal.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.components.BannerTone
import com.ledger.personal.ui.components.IconBadge
import com.ledger.personal.ui.components.OtpField
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.components.StatusBanner
import com.ledger.personal.ui.components.TextLink
import com.ledger.personal.ui.security.SecureWindow
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** State for OTP login screen. */
data class OtpLoginUiState(
    val phone: String,
    val demoOtp: Boolean = false,
)

/** Shows the OTP login screen for returning users with landscape scrollability and StateFlow countdown. */
@Composable
fun OtpLoginScreen(
    uiState: OtpLoginUiState,
    resendCountdown: Int = AppConstants.OTP_RESEND_COOLDOWN_SECONDS,
    onResend: () -> Unit = {},
    onVerifyOtp: (String, (Boolean) -> Unit) -> Unit,
    onReset: () -> Unit,
) {
    val phone = uiState.phone
    val demoOtp = uiState.demoOtp
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }
    var checking by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    SecureWindow()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .safeDrawingPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(24.dp))
                IconBadge(Icons.Outlined.Lock, size = 56)
                Text(
                    "Welcome back",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Enter the 6-digit OTP sent to +91 $phone",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OtpField(code, onValueChange = {
                    code = it
                    wrong = false
                })
                if (wrong) {
                    StatusBanner("That code doesn't match. Check the SMS and try again.", BannerTone.ERROR)
                }
                if (demoOtp) {
                    StatusBanner("Demo build: use code ${MockValues.OTP}.", BannerTone.INFO)
                }
                if (resendCountdown > 0) {
                    Text(
                        "Resend code in 0:${resendCountdown.toString().padStart(2, '0')}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    TextLink("Resend code", onResend)
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton(
                    "Verify and sign in",
                    onClick = {
                        checking = true
                        onVerifyOtp(code) { ok ->
                            checking = false
                            wrong = !ok
                        }
                    },
                    enabled = code.length == AppConstants.OTP_CODE_LENGTH && !checking,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextLink("Sign in with another number", { confirmReset = true })
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Switch account?") },
            text = { Text("You'll be taken to the phone verification screen to sign in with a different mobile number.") },
            confirmButton = { TextButton(onClick = onReset) { Text("Switch") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

/** Overload accepting individual parameters delegating to [OtpLoginUiState]. */
@Composable
fun OtpLoginScreen(
    phone: String,
    onVerifyOtp: (String, (Boolean) -> Unit) -> Unit,
    onReset: () -> Unit,
    demoOtp: Boolean = false,
    resendCountdown: Int = AppConstants.OTP_RESEND_COOLDOWN_SECONDS,
    onResend: () -> Unit = {},
) = OtpLoginScreen(
    uiState = OtpLoginUiState(phone = phone, demoOtp = demoOtp),
    resendCountdown = resendCountdown,
    onResend = onResend,
    onVerifyOtp = onVerifyOtp,
    onReset = onReset,
)

@PreviewLightDark
@Composable
private fun OtpLoginPreview() {
    PersonalLedgerTheme {
        OtpLoginScreen(
            phone = MockValues.DEFAULT_PHONE,
            onVerifyOtp = { _, done -> done(true) },
            onReset = {},
            demoOtp = true,
        )
    }
}
