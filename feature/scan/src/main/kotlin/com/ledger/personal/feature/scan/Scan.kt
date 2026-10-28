@file:Suppress("MatchingDeclarationName")

package com.ledger.personal.ui.app.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.core.content.ContextCompat
import com.ledger.personal.core.upiPayload
import com.ledger.personal.data.tools.UpiTarget
import com.ledger.personal.data.tools.parseUpi
import com.ledger.personal.ui.components.BannerTone
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.IconBadge
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.components.StatusBanner
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** UI state for the scan screen. */
data class ScanUiState(
    val liveCamera: Boolean = false,
    val cameraGranted: Boolean = false,
    val invalid: Boolean = false,
    val manual: String = "",
)

/** Shows the scan screen. */
@Composable
fun ScanScreen(liveCamera: Boolean, onBack: () -> Unit, onTarget: (UpiTarget) -> Unit) {
    val context = LocalContext.current
    var granted by rememberSaveable {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    var manual by rememberSaveable { mutableStateOf("") }
    var invalid by rememberSaveable { mutableStateOf(false) }
    val accept: (String) -> Unit = { raw ->
        val target = parseUpi(raw)
        invalid = target == null
        target?.let(onTarget)
    }
    LaunchedEffect(liveCamera) { if (liveCamera && !granted) launcher.launch(Manifest.permission.CAMERA) }
    val uiState = ScanUiState(
        liveCamera = liveCamera,
        cameraGranted = granted,
        invalid = invalid,
        manual = manual,
    )
    ScanForm(
        uiState = uiState,
        onManual = { manual = it },
        onAccept = accept,
        onAllowCamera = { launcher.launch(Manifest.permission.CAMERA) },
        onBack = onBack,
    )
}

/** Scan layout. [uiState] drives rendering of each branch. */
@Composable
private fun ScanForm(
    uiState: ScanUiState,
    onManual: (String) -> Unit,
    onAccept: (String) -> Unit,
    onAllowCamera: () -> Unit,
    onBack: () -> Unit,
) {
    FeatureScaffold("Scan to pay", onBack, subtitle = "Any UPI QR code") {
        if (uiState.liveCamera && uiState.cameraGranted) {
            CameraScanner(onCode = onAccept)
        } else {
            IconBadge(Icons.Outlined.QrCodeScanner, size = 120)
            Text(
                if (uiState.liveCamera) {
                    "Allow camera access to scan QR codes."
                } else {
                    "The demo build has no camera feed. Use the sample QR below."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.liveCamera) OutlinedButton(onClick = onAllowCamera) { Text("Allow camera") }
        }
        if (uiState.invalid) StatusBanner("That isn't a UPI payment code. Ask the payee for their UPI QR.", BannerTone.ERROR)
        OutlinedTextField(
            uiState.manual,
            onManual,
            label = { Text("Or paste a UPI link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            "Continue",
            onClick = { onAccept(uiState.manual) },
            enabled = uiState.manual.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        if (!uiState.liveCamera) {
            val sample = upiPayload("chai.point@okicici", 12_000) + "&pn=Chai%20Point"
            OutlinedButton(onClick = { onAccept(sample) }, modifier = Modifier.fillMaxWidth()) {
                Text("Use sample QR (Chai Point, ₹120)")
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ScanDemoPreview() {
    PersonalLedgerTheme { ScanPreviewContent(ScanUiState(liveCamera = false, cameraGranted = false, invalid = false)) }
}

@PreviewLightDark
@Composable
private fun ScanPermissionPreview() {
    PersonalLedgerTheme { ScanPreviewContent(ScanUiState(liveCamera = true, cameraGranted = false, invalid = false)) }
}

@PreviewLightDark
@Composable
private fun ScanRejectedPreview() {
    PersonalLedgerTheme {
        ScanPreviewContent(
            ScanUiState(
                liveCamera = false,
                cameraGranted = false,
                invalid = true,
                manual = "not-a-upi-link",
            ),
        )
    }
}

@Composable
private fun ScanPreviewContent(uiState: ScanUiState) {
    ScanForm(uiState, onManual = {}, onAccept = {}, onAllowCamera = {}, onBack = {})
}
