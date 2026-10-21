package com.ledger.personal.ui.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.OtpField
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.TextLink
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.delay

private const val RESEND_SECONDS = 30
private const val OTP_LENGTH = 6

/** Shows OTP verification step. */
@Composable
fun OtpStep(
    phone: String,
    sentMessage: String = "OTP sent",
    onEdit: () -> Unit,
    onResend: (((Result<String>) -> Unit) -> Unit)? = null,
    onVerified: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var seconds by rememberSaveable { mutableIntStateOf(RESEND_SECONDS) }
    var feedbackMessage by rememberSaveable { mutableStateOf<String?>(sentMessage) }
    var isResending by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(seconds) {
        if (seconds > 0) {
            delay(1_000)
            seconds -= 1
        }
    }
    Text("Enter the code", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text("Sent to +91 $phone", color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextLink("Change number", onEdit)
    OtpField(code, {
        code = it
        error = false
    })
    if (error) StatusBanner("That code doesn't match. Check the SMS and try again.", BannerTone.ERROR)
    feedbackMessage?.let { msg ->
        StatusBanner(msg, BannerTone.INFO)
    }
    StatusBanner("Demo build: use code ${MockValues.OTP}.", BannerTone.INFO)
    if (seconds > 0) {
        Text("Resend code in 0:${seconds.toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        TextLink(if (isResending) "Resending..." else "Resend code", {
            if (!isResending) {
                seconds = RESEND_SECONDS
                if (onResend != null) {
                    isResending = true
                    onResend { result ->
                        isResending = false
                        result.fold(
                            onSuccess = { msg ->
                                feedbackMessage = msg.ifBlank { "OTP sent" }
                            },
                            onFailure = {
                                feedbackMessage = it.message ?: "Failed to resend OTP"
                            },
                        )
                    }
                }
            }
        })
    }
    PrimaryButton(
        "Verify",
        onClick = {
            if (code == MockValues.OTP) onVerified() else error = true
        },
        enabled = code.length == OTP_LENGTH,
        modifier = Modifier.fillMaxWidth(),
    )
}

@PreviewLightDark
@Composable
private fun OtpStepPreview() {
    PersonalLedgerTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                OtpStep(MockValues.DEFAULT_PHONE, onEdit = {}, onVerified = {})
            }
        }
    }
}
