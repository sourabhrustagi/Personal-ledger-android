package com.ledger.personal.ui.feature

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val documents = listOf("PAN card", "Aadhaar", "Passport")
private val stepTitles = listOf("Your details", "Scan document", "Review")

@Composable
fun KycScreen(onBack: () -> Unit, liveCapture: Boolean = false, viewModel: KycViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var step by rememberSaveable { mutableIntStateOf(0) }
    var document by rememberSaveable { mutableStateOf(documents.first()) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    FeatureScaffold("Verify identity", onBack, subtitle = if (submitted) null else "Step ${step + 1} of 3 · ${stepTitles[step]}") {
        if (submitted) {
            Submitted(state.humanReview, onBack)
            return@FeatureScaffold
        }
        LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth())
        state.conflict?.let {
            StatusBanner("Your name was changed on another device. Check it before you continue.", BannerTone.WARNING, title = it)
        }
        when (step) {
            0 -> DetailsStep(state.name, viewModel::onName, document, { document = it }) { step = 1 }
            1 -> DocumentStep(state, document, liveCapture, viewModel::capture, onBack = { step = 0 }) { step = 2 }
            else -> ReviewStep(state, document, onEdit = { step = 0 }) { submitted = true }
        }
    }
}

@Composable
private fun DetailsStep(name: String, onName: (String) -> Unit, document: String, onDocument: (String) -> Unit, onNext: () -> Unit) {
    SectionCard(title = "Tell us about you") {
        Text(
            "Enter your name exactly as it appears on your ID.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            label = { Text("Full legal name") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    SectionCard(title = "Document type") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            documents.forEach { option ->
                FilterChip(selected = document == option, onClick = { onDocument(option) }, label = { Text(option) })
            }
        }
    }
    StatusBanner("Your progress is saved on this device, so you can finish later.", BannerTone.INFO)
    PrimaryButton("Continue", onClick = onNext, enabled = name.trim().length >= 3)
}

@Composable
private fun DocumentStep(
    state: KycUiState,
    document: String,
    liveCapture: Boolean,
    onCapture: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val context = LocalContext.current
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { cameraGranted = it }
    LaunchedEffect(liveCapture) { if (liveCapture && !cameraGranted) launcher.launch(Manifest.permission.CAMERA) }

    Text("Place your $document inside the frame", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().aspectRatio(1.586f)
            .border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(20.dp)),
    ) {
        if (liveCapture && cameraGranted) {
            val owner = LocalLifecycleOwner.current
            val preview = remember { PreviewView(context) }
            AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
            LaunchedEffect(Unit) { CameraXDocumentCapture(context).bind(owner, preview) }
        } else {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconBadge(if (state.captureUri != null) Icons.Outlined.CheckCircle else Icons.Outlined.Badge, size = 64)
                    Text(if (state.captureUri != null) "$document captured" else "Good light, no glare, all four corners visible")
                }
            }
        }
    }
    if (state.captureUri != null) {
        StatusBanner(
            state.edge?.let { "Edges detected. The photo is sharp enough to read." } ?: "We couldn't find the document edges. Try again.",
            if (state.edge != null) BannerTone.SUCCESS else BannerTone.WARNING,
        )
        PrimaryButton("Use this photo", onClick = onNext, enabled = state.edge != null)
        OutlinedButton(onClick = onCapture, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Text("Retake") }
    } else {
        PrimaryButton("Capture", onClick = onCapture, icon = Icons.Outlined.CameraAlt)
    }
    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
}

@Composable
private fun ReviewStep(state: KycUiState, document: String, onEdit: () -> Unit, onSubmit: () -> Unit) {
    SectionCard(title = "Check your details") {
        DetailRow("Full name", state.name)
        DetailRow("Document", document)
        DetailRow("Photo", if (state.captureUri != null) "Captured" else "Missing")
        TextButton(onClick = onEdit) { Text("Edit details") }
    }
    if (state.humanReview) {
        StatusBanner("Some details were hard to read, so a person will check them. This usually takes under 24 hours.", BannerTone.WARNING)
    }
    Text(
        "By submitting, you confirm the details are yours and agree to identity verification under RBI KYC rules.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    PrimaryButton("Submit for verification", onClick = onSubmit, enabled = state.captureUri != null && state.name.isNotBlank())
}

@Composable
private fun Submitted(humanReview: Boolean, onDone: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(Icons.Outlined.HourglassTop, size = 88)
        Text("Verification in progress", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            if (humanReview) "A specialist is reviewing your documents. We'll notify you within 24 hours." else
                "We're checking your documents. This usually takes a few minutes.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Box(Modifier.height(16.dp))
    PrimaryButton("Back to home", onClick = onDone)
}
