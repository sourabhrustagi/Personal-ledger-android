package com.ledger.personal.ui.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.OtpField
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.TextLink
import com.ledger.personal.ui.security.SecureWindow
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.delay

private const val RESEND_SECONDS = 30
private const val OTP_LENGTH = 6

/** Shows the OTP login screen for returning users. */
@Composable
fun OtpLoginScreen(
    phone: String,
    onVerifyOtp: (String, (Boolean) -> Unit) -> Unit,
    onReset: () -> Unit,
    demoOtp: Boolean = false,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var seconds by rememberSaveable { mutableIntStateOf(RESEND_SECONDS) }

    LaunchedEffect(seconds) {
        if (seconds > 0) {
            delay(1_000)
            seconds -= 1
        }
    }

    SecureWindow()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
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
            if (seconds > 0) {
                Text(
                    "Resend code in 0:${seconds.toString().padStart(2, '0')}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TextLink("Resend code", { seconds = RESEND_SECONDS })
            }
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                "Verify and sign in",
                onClick = {
                    checking = true
                    onVerifyOtp(code) { ok ->
                        checking = false
                        wrong = !ok
                    }
                },
                enabled = code.length == OTP_LENGTH && !checking,
                modifier = Modifier.fillMaxWidth(),
            )
            TextLink("Sign in with another number", { confirmReset = true })
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
