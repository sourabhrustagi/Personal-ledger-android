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
import com.ledger.personal.ui.feature.SecureWindow
import com.ledger.personal.ui.feature.StepHeader
import com.ledger.personal.ui.theme.PersonalLedgerTheme

private val steps = listOf("Phone", "Verify", "Create MPIN", "Confirm MPIN", "Secure device")

/** Shows onboarding flow orchestrating the registration wizard. */
@Composable
fun OnboardingFlow(
    onFinish: (name: String, phone: String, pin: String, biometric: Boolean, language: String) -> Unit,
    onBind: ((Result<String>) -> Unit) -> Unit,
) {
    var step by rememberSaveable { mutableStateOf(OnboardStep.WELCOME) }
    var language by rememberSaveable { mutableStateOf("English") }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    SecureWindow()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (step != OnboardStep.WELCOME) StepHeader(steps, step.ordinal - 1)
            when (step) {
                OnboardStep.WELCOME -> WelcomeStep(language, { language = it }) { step = OnboardStep.PHONE }
                OnboardStep.PHONE -> PhoneStep(name, phone, { name = it }, { phone = it }) { step = OnboardStep.OTP }
                OnboardStep.OTP -> OtpStep(phone, onEdit = { step = OnboardStep.PHONE }) { step = OnboardStep.MPIN }
                OnboardStep.MPIN -> PinStep("Create your 4-digit MPIN", "You'll use it to open LedgerOne and approve payments.") {
                    pin = it
                    step = OnboardStep.CONFIRM
                }
                OnboardStep.CONFIRM -> PinStep("Confirm your MPIN", "Enter the same 4 digits again.", expected = pin, onMismatch = {
                    step = OnboardStep.MPIN
                }) { step = OnboardStep.BIOMETRIC }
                OnboardStep.BIOMETRIC -> BiometricStep(onBind) { biometric -> onFinish(name.trim(), phone, pin, biometric, language) }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun OnboardingPreview() {
    PersonalLedgerTheme { OnboardingFlow(onFinish = { _, _, _, _, _ -> }, onBind = {}) }
}
