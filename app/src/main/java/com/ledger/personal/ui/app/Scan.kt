package com.ledger.personal.ui.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.ledger.personal.core.upiPayload
import com.ledger.personal.data.tools.UpiTarget
import com.ledger.personal.data.tools.parseUpi
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.StatusBanner
import java.util.concurrent.Executors

/** Shows the scan screen. */
@Composable
fun ScanScreen(liveCamera: Boolean, onBack: () -> Unit, onTarget: (UpiTarget) -> Unit) {
    val context = LocalContext.current
    var granted by remember {
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
    FeatureScaffold("Scan to pay", onBack, subtitle = "Any UPI QR code") {
        if (liveCamera && granted) {
            CameraScanner(onCode = accept)
        } else {
            IconBadge(Icons.Outlined.QrCodeScanner, size = 120)
            Text(
                if (liveCamera) "Allow camera access to scan QR codes." else "The demo build has no camera feed. Use the sample QR below.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (liveCamera) OutlinedButton(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
        }
        if (invalid) StatusBanner("That isn't a UPI payment code. Ask the payee for their UPI QR.", BannerTone.ERROR)
        OutlinedTextField(
            manual,
            { manual = it },
            label = { Text("Or paste a UPI link") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton("Continue", onClick = { accept(manual) }, enabled = manual.isNotBlank(), modifier = Modifier.fillMaxWidth())
        if (!liveCamera) {
            val sample = upiPayload("chai.point@okicici", 12_000) + "&pn=Chai%20Point"
            OutlinedButton(onClick = { accept(sample) }, modifier = Modifier.fillMaxWidth()) {
                Text("Use sample QR (Chai Point, ₹120)")
            }
        }
    }
}

/** Shows camera scanner. */
@Composable
private fun CameraScanner(onCode: (String) -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val preview = remember { PreviewView(context) }
    var done by remember { mutableStateOf(false) }
    AndroidView(
        factory = { preview },
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(MaterialTheme.shapes.medium),
    )
    Text("Point the camera at the QR code", color = MaterialTheme.colorScheme.onSurfaceVariant)
    DisposableEffect(owner) {
        val executor = Executors.newSingleThreadExecutor()
        bindQrScanner(context, owner, preview, executor) { raw ->
            if (!done) {
                done = true
                ContextCompat.getMainExecutor(context).execute { onCode(raw) }
            }
        }
        onDispose { executor.shutdown() }
    }
}

/** Binds qr scanner. */
@OptIn(ExperimentalGetImage::class)
private fun bindQrScanner(
    context: Context,
    owner: LifecycleOwner,
    previewView: PreviewView,
    executor: java.util.concurrent.Executor,
    onCode: (String) -> Unit,
) {
    val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
    analysis.setAnalyzer(executor) { proxy ->
        val media = proxy.image
        if (media == null) {
            proxy.close()
            return@setAnalyzer
        }
        scanner.process(InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { codes -> codes.firstNotNullOfOrNull { it.rawValue }?.let(onCode) }
            .addOnCompleteListener { proxy.close() }
    }
    val future = ProcessCameraProvider.getInstance(context)
    future.addListener({
        val provider = future.get()
        val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
        provider.unbindAll()
        provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
    }, ContextCompat.getMainExecutor(context))
}

/** Shows the force update screen. */
@Composable
fun ForceUpdateScreen(versionName: String) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            IconBadge(Icons.Outlined.SystemUpdate, size = 72)
            Text("Update required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Version $versionName is no longer supported. Update LedgerOne to keep your account secure and keep paying.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusBanner("Your money and data are safe. Nothing is lost by updating.", BannerTone.INFO)
            PrimaryButton(
                "Update now",
                onClick = { openStore(context) },
                icon = Icons.Outlined.SystemUpdate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Opens store. */
private fun openStore(context: Context) {
    val id = context.packageName.removeSuffix(".debug")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")))
    }
}
