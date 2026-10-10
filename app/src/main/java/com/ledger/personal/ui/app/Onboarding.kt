package com.ledger.personal.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.NumericKeypad
import com.ledger.personal.ui.feature.OtpField
import com.ledger.personal.ui.feature.PinDots
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.SecureWindow
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.StepHeader
import com.ledger.personal.ui.feature.TextLink
import kotlinx.coroutines.delay

/** Allowed values of onboard step. */
private enum class OnboardStep { WELCOME, PHONE, OTP, MPIN, CONFIRM, BIOMETRIC }

private val steps = listOf("Phone", "Verify", "Create MPIN", "Confirm MPIN", "Secure device")
private val languages = listOf("English", "हिन्दी", "தமிழ்", "తెలుగు")
const val MOCK_OTP = "123456"
private const val MPIN_LENGTH = 4
private const val RESEND_SECONDS = 30

/** Shows onboarding flow. */
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
                OnboardStep.WELCOME -> Welcome(language, { language = it }) { step = OnboardStep.PHONE }
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

/** Shows welcome. */
@Composable
private fun Welcome(language: String, onLanguage: (String) -> Unit, onStart: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    IconBadge(Icons.Outlined.AccountBalance, size = 64)
    Text("LedgerOne", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(
        "Your money, payments and accounts in one secure place.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("Choose language", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        languages.forEach { option ->
            FilterChip(selected = option == language, onClick = { onLanguage(option) }, label = { Text(option) })
        }
    }
    StatusBanner("Bank-grade encryption. We never share your data without consent.", BannerTone.INFO, title = "Secure by design")
    PrimaryButton("Get started", onClick = onStart, modifier = Modifier.fillMaxWidth())
}

/** Shows phone step. */
@Composable
private fun PhoneStep(name: String, phone: String, onName: (String) -> Unit, onPhone: (String) -> Unit, onNext: () -> Unit) {
    var consent by rememberSaveable { mutableStateOf(false) }
    Text("Let's get you set up", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    OutlinedTextField(name, onName, label = { Text("Full name (as on PAN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(
        phone,
        { next -> if (next.length <= 10 && next.all(Char::isDigit)) onPhone(next) },
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
        enabled = consent && phone.length == 10 && name.isNotBlank(),
        icon = Icons.Outlined.Sms,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Shows otp step. */
@Composable
private fun OtpStep(phone: String, onEdit: () -> Unit, onVerified: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf(false) }
    var seconds by rememberSaveable { mutableIntStateOf(RESEND_SECONDS) }
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
    StatusBanner("Demo build: use code $MOCK_OTP.", BannerTone.INFO)
    if (seconds > 0) {
        Text("Resend code in 0:${seconds.toString().padStart(2, '0')}", color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        TextLink("Resend code", { seconds = RESEND_SECONDS })
    }
    PrimaryButton("Verify", onClick = {
        if (code == MOCK_OTP) onVerified() else error = true
    }, enabled = code.length == 6, modifier = Modifier.fillMaxWidth())
}

/** Shows pin step. */
@Composable
private fun PinStep(
    title: String,
    message: String,
    expected: String? = null,
    onMismatch: () -> Unit = {},
    onDone: (String) -> Unit,
) {
    var entry by rememberSaveable(title) { mutableStateOf("") }
    var mismatch by rememberSaveable(title) { mutableStateOf(false) }
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PinDots(entry.length, MPIN_LENGTH, Modifier.padding(vertical = 24.dp), error = mismatch)
    }
    if (mismatch) StatusBanner("MPINs didn't match. Create it again.", BannerTone.ERROR)
    if (expected == null) Text("Avoid 1234, 0000 or your birth year.", style = MaterialTheme.typography.bodySmall)
    NumericKeypad(
        onDigit = { digit ->
            if (entry.length < MPIN_LENGTH) entry += digit
            if (entry.length == MPIN_LENGTH) {
                when {
                    expected == null || entry == expected -> onDone(entry)
                    else -> {
                        mismatch = true
                        entry = ""
                        onMismatch()
                    }
                }
            }
        },
        onDelete = { entry = entry.dropLast(1) },
    )
}

/** Shows biometric step. */
@Composable
private fun BiometricStep(onBind: ((Result<String>) -> Unit) -> Unit, onDone: (Boolean) -> Unit) {
    val context = LocalContext.current
    val available = biometricAvailable(context)
    var bindError by rememberSaveable { mutableStateOf<String?>(null) }
    IconBadge(Icons.Outlined.Fingerprint, size = 64)
    Text("Unlock faster with biometrics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(
        "Use your fingerprint or face to open the app. Payments above ₹5,000 still ask for your MPIN.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    StatusBanner("This phone will be bound to your account. Signing in elsewhere unbinds it.", BannerTone.INFO, title = "Device binding")
    bindError?.let { StatusBanner("$it. You can try again or bind later from Security.", BannerTone.ERROR, title = "Binding failed") }
    if (available) {
        PrimaryButton("Enable biometrics", onClick = {
            bindError = null
            onBind { result -> if (result.isSuccess) onDone(true) else bindError = result.exceptionOrNull()?.message ?: "Cancelled" }
        }, icon = Icons.Outlined.Fingerprint, modifier = Modifier.fillMaxWidth())
    } else {
        StatusBanner("No biometrics enrolled on this device. You can turn this on later in Security.", BannerTone.WARNING)
    }
    OutlinedButton(onClick = { onDone(false) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
        Text(if (available) "Not now" else "Continue")
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconBadge(Icons.Outlined.Shield, size = 32)
        Text("You can finish KYC from Profile to unlock higher limits.", style = MaterialTheme.typography.bodySmall)
    }
}
