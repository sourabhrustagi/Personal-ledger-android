package com.ledger.personal.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.components.StepHeader
import com.ledger.personal.ui.security.SecureWindow
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Steps in the onboarding flow. */
private enum class OnboardStep { PHONE, OTP }

/** Shows onboarding flow orchestrating phone entry and OTP verification. */
@Composable
fun OnboardingFlow(
    otpState: OtpUiState = OtpUiState(),
    resendCountdown: Int = otpState.resendCountdown,
    onSendOtpClick: (phone: String) -> Unit = {},
    onEditPhoneClick: () -> Unit = {},
    onResendOtpClick: (phone: String) -> Unit = {},
    onVerifyOtpClick: (code: String, phone: String) -> Unit = { _, _ -> },
    onDismissError: () -> Unit = {},
) {
    val steps = listOf(
        stringResource(AppStrings.STEP_PHONE),
        stringResource(AppStrings.STEP_VERIFY),
    )
    var phone by rememberSaveable { mutableStateOf(otpState.phone) }

    val step = if (otpState.isOtpSent) OnboardStep.OTP else OnboardStep.PHONE

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
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StepHeader(steps, step.ordinal)
                when (step) {
                    OnboardStep.PHONE -> PhoneStep(
                        phone = phone,
                        onPhone = { phone = it },
                        uiState = otpState,
                        onSendClick = { onSendOtpClick(phone) },
                    )
                    OnboardStep.OTP -> OtpStep(
                        phone = phone,
                        uiState = otpState,
                        resendCountdown = resendCountdown,
                        onEdit = onEditPhoneClick,
                        onResend = { onResendOtpClick(phone) },
                        onVerifyClick = { code -> onVerifyOtpClick(code, phone) },
                        onDismissError = onDismissError,
                    )
                }
            }
        }
    }
}

/** Backward-compatible overload for legacy callers and previews. */
@Composable
fun OnboardingFlow(
    otpState: OtpUiState = OtpUiState(),
    resendCountdown: Int = otpState.resendCountdown,
    onSendOtp: (phone: String) -> Unit = {},
    onFinish: (phone: String) -> Unit,
) = OnboardingFlow(
    otpState = otpState,
    resendCountdown = resendCountdown,
    onSendOtpClick = onSendOtp,
    onEditPhoneClick = {},
    onResendOtpClick = onSendOtp,
    onVerifyOtpClick = { code, phone ->
        if (code == MockValues.OTP) onFinish(phone)
    },
)

@PreviewLightDark
@Composable
private fun OnboardingPreview() {
    PersonalLedgerTheme { OnboardingFlow(onFinish = {}) }
}
