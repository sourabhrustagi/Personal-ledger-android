package com.ledger.personal.ui.app.auth

import com.ledger.personal.mock.MockValues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.app.MAX_MPIN_ATTEMPTS
import com.ledger.personal.ui.app.SessionState
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.NumericKeypad
import com.ledger.personal.ui.feature.PinDots
import com.ledger.personal.ui.feature.SecureWindow
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.TextLink
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 4

/** Shows the mpin login screen. */
@Composable
fun MpinLoginScreen(
    name: String,
    biometric: Boolean,
    session: SessionState,
    onPin: (String, (Boolean) -> Unit) -> Unit,
    onBiometric: ((Boolean) -> Unit) -> Unit,
    onReset: () -> Unit,
    demoPin: Boolean = false,
) {
    val context = LocalContext.current
    var entry by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lockedSeconds = ((session.lockedUntilMs - now) / 1_000).coerceAtLeast(0)
    LaunchedEffect(session.lockedUntilMs, now) {
        if (session.lockedUntilMs > now) {
            delay(1_000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(biometric) {
        if (biometric && biometricAvailable(context)) {
            promptBiometric(context, "Unlock LedgerOne") { ok ->
                if (!ok) return@promptBiometric
                checking = true
                onBiometric { signedIn ->
                    checking = false
                    wrong = !signedIn
                }
            }
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
                if (name.isBlank()) "Welcome back" else "Welcome back, ${name.substringBefore(' ')}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (demoPin) "Enter your MPIN. Demo: ${MockValues.MPIN}" else "Enter your MPIN",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PinDots(entry.length, PIN_LENGTH, Modifier.padding(vertical = 16.dp), error = wrong)
            when {
                lockedSeconds > 0 -> StatusBanner(
                    "Too many attempts. Try again in $lockedSeconds s, or reset with OTP.",
                    BannerTone.ERROR,
                    title = "MPIN locked",
                )
                session.signInMessage != null -> StatusBanner(session.signInMessage, BannerTone.ERROR)
                wrong -> StatusBanner(
                    "Incorrect MPIN. ${MAX_MPIN_ATTEMPTS - session.failedAttempts} attempts left before a 30 s lock.",
                    BannerTone.ERROR,
                )
            }
            Spacer(Modifier.weight(1f))
            NumericKeypad(
                onDigit = { digit ->
                    if (lockedSeconds > 0 || checking || entry.length >= PIN_LENGTH) return@NumericKeypad
                    entry += digit
                    if (entry.length == PIN_LENGTH) {
                        val typed = entry
                        checking = true
                        now = System.currentTimeMillis()
                        onPin(typed) { ok ->
                            checking = false
                            wrong = !ok
                            entry = ""
                        }
                    }
                },
                onDelete = { entry = entry.dropLast(1) },
                large = true,
                leftKey = if (biometric) {
                    {
                        IconButton(onClick = {
                            promptBiometric(context, "Unlock LedgerOne") { ok ->
                                if (!ok) return@promptBiometric
                                checking = true
                                onBiometric { signedIn ->
                                    checking = false
                                    wrong = !signedIn
                                }
                            }
                        }) {
                            Icon(Icons.Outlined.Fingerprint, contentDescription = "Unlock with biometrics")
                        }
                    }
                } else {
                    null
                },
            )
            TextLink("Forgot MPIN?", { confirmReset = true })
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset MPIN?") },
            text = { Text("You'll verify your phone with an OTP and set a new MPIN. Data on this device is cleared.") },
            confirmButton = { TextButton(onClick = onReset) { Text("Reset") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginPreview() {
    PersonalLedgerTheme {
        MpinLoginScreen(
            name = "Asha",
            biometric = false,
            session = SessionState(),
            onPin = { _, done -> done(true) },
            onBiometric = {},
            onReset = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginBiometricPreview() {
    PersonalLedgerTheme {
        MpinLoginScreen(
            name = "Asha",
            biometric = true,
            session = SessionState(),
            onPin = { _, done -> done(true) },
            onBiometric = {},
            onReset = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginLockedPreview() {
    PersonalLedgerTheme {
        MpinLoginScreen(
            name = "Asha",
            biometric = false,
            session = SessionState(failedAttempts = 3, lockedUntilMs = System.currentTimeMillis() + 20_000),
            onPin = { _, done -> done(false) },
            onBiometric = {},
            onReset = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginFailedPreview() {
    PersonalLedgerTheme {
        MpinLoginScreen(
            name = "Asha",
            biometric = false,
            session = SessionState(failedAttempts = 2, signInMessage = "Couldn't reach the bank."),
            onPin = { _, done -> done(false) },
            onBiometric = {},
            onReset = {},
        )
    }
}
