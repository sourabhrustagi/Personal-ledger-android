package com.ledger.personal.ui.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.session.OtpResponse
import com.ledger.personal.ui.feature.StepHeader
import com.ledger.personal.ui.security.SecureWindow
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Steps in the onboarding flow. */
private enum class OnboardStep { PHONE, OTP }

private val steps = listOf("Phone", "Verify")

/** Shows onboarding flow orchestrating phone entry and OTP verification. */
@Composable
fun OnboardingFlow(
    onSendOtp: suspend (phone: String) -> Result<OtpResponse> = { Result.success(OtpResponse()) },
    onFinish: (phone: String) -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(OnboardStep.PHONE) }
    var phone by rememberSaveable { mutableStateOf("") }
    var otpMessage by rememberSaveable { mutableStateOf("OTP sent") }
    SecureWindow()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StepHeader(steps, step.ordinal)
            when (step) {
                OnboardStep.PHONE -> PhoneStep(
                    phone = phone,
                    onPhone = { phone = it },
                    onSendCode = onSendOtp,
                    onNext = { response ->
                        otpMessage = response.message.ifBlank { "OTP sent" }
                        step = OnboardStep.OTP
                    },
                )
                OnboardStep.OTP -> OtpStep(
                    phone = phone,
                    sentMessage = otpMessage,
                    onEdit = { step = OnboardStep.PHONE },
                    onResend = { onSendOtp(phone) },
                    onVerified = { onFinish(phone) },
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingPreview() {
    PersonalLedgerTheme { OnboardingFlow(onFinish = {}) }
}
