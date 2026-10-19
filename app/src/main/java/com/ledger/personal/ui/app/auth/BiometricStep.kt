package com.ledger.personal.ui.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows biometric and device binding step. */
@Composable
fun BiometricStep(onBind: ((Result<String>) -> Unit) -> Unit, onDone: (Boolean) -> Unit) {
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

@PreviewLightDark
@Composable
private fun BiometricStepPreview() {
    PersonalLedgerTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                BiometricStep(onBind = {}, onDone = {})
            }
        }
    }
}
