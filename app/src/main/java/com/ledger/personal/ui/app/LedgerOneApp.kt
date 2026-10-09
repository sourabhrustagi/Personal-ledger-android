package com.ledger.personal.ui.app

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.BuildConfig
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.ui.feature.FraudScreen
import com.ledger.personal.ui.feature.PosScreen
import com.ledger.personal.ui.navigation.LedgerNavHost
import com.ledger.personal.ui.theme.LedgerOneTheme
import com.ledger.personal.ui.theme.ThemeMode

private const val LOCK_AFTER_MS = 30_000L

@Composable
fun LedgerOneApp(viewModel: AppViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val forceUpdate by MockSwitches.forceUpdate.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LockWhenBackgrounded(viewModel::lock)
    LedgerOneTheme(prefs?.theme ?: ThemeMode.SYSTEM) {
        val current = prefs
        when {
            current == null -> Box(Modifier.fillMaxSize()) { Surface(Modifier.fillMaxSize()) {} }
            forceUpdate -> ForceUpdateScreen(BuildConfig.VERSION_NAME)
            !current.onboarded -> OnboardingFlow(viewModel::completeOnboarding) { done -> viewModel.bindDevice(context, done) }
            !session.unlocked -> MpinLoginScreen(
                name = current.name,
                biometric = current.biometric,
                session = session,
                onPin = { pin, done -> viewModel.tryMpin(pin, onResult = done) },
                onBiometric = viewModel::unlockWithBiometric,
                onReset = viewModel::signOutEverywhere,
                demoPin = BuildConfig.USE_FAKE_REPOSITORY,
            )
            session.mode == AppMode.POS -> ModeFrame(AppMode.POS, { viewModel.setMode(AppMode.PERSONAL) }) {
                PosScreen(onBack = { viewModel.setMode(AppMode.PERSONAL) })
            }
            session.mode == AppMode.FRAUD -> ModeFrame(AppMode.FRAUD, { viewModel.setMode(AppMode.PERSONAL) }) {
                FraudScreen(onBack = { viewModel.setMode(AppMode.PERSONAL) }, role = "ANALYST")
            }
            else -> LedgerNavHost(prefs = current, appViewModel = viewModel)
        }
    }
}

@Composable
private fun LockWhenBackgrounded(onLock: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val activity = LocalContext.current as? Activity
    DisposableEffect(owner) {
        var stoppedAt = 0L
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> if (activity?.isChangingConfigurations != true) stoppedAt = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> {
                    if (stoppedAt != 0L && System.currentTimeMillis() - stoppedAt > LOCK_AFTER_MS) onLock()
                    stoppedAt = 0L
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
private fun ModeFrame(mode: AppMode, onExit: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            Surface(color = MaterialTheme.colorScheme.inverseSurface, contentColor = MaterialTheme.colorScheme.inverseOnSurface) {
                Row(Modifier.statusBarsPadding().padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (mode == AppMode.POS) Icons.Outlined.PointOfSale else Icons.Outlined.GppGood, contentDescription = null)
                    Text(
                        "${mode.label} mode",
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(start = 8.dp),
                    )
                    TextButton(onClick = onExit) { Text("Exit", color = MaterialTheme.colorScheme.inverseOnSurface) }
                }
            }
        },
    ) { inner ->
        Box(Modifier.padding(inner).consumeWindowInsets(inner)) { content() }
    }
}
