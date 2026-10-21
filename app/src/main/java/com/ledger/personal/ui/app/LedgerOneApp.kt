package com.ledger.personal.ui.app

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.app.auth.OnboardingFlow
import com.ledger.personal.ui.app.auth.OtpLoginScreen
import com.ledger.personal.ui.app.update.ForceUpdateScreen
import com.ledger.personal.ui.app.update.SoftUpdateDialog
import com.ledger.personal.ui.navigation.LedgerNavHost
import com.ledger.personal.ui.theme.LedgerOneTheme
import com.ledger.personal.ui.theme.ThemeMode

private const val LOCK_AFTER_MS = 30_000L

/** Picks splash, update, onboarding, lock, or the signed-in app. */
@Composable
fun LedgerOneApp(viewModel: AppViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LockWhenBackgrounded(viewModel::lock)
    LedgerOneTheme(prefs?.theme ?: ThemeMode.SYSTEM) {
        when (val screen = status) {
            AppStatus.Loading -> Surface(Modifier.fillMaxSize()) {}
            AppStatus.Update -> ForceUpdateScreen(BuildConfig.VERSION_NAME)
            is AppStatus.Onboard -> Onboard(viewModel)
            is AppStatus.Locked -> Locked(screen.prefs, viewModel)
            is AppStatus.Personal -> LedgerNavHost(screen.prefs, viewModel)
        }
        if (status.softUpdate) {
            SoftUpdateDialog(onUpdate = viewModel::dismissUpdateOffer, onLater = viewModel::ignoreUpdate)
        }
    }
}

/** Runs onboarding with phone and OTP. */
@Composable
private fun Onboard(viewModel: AppViewModel) {
    OnboardingFlow(
        onSendOtp = viewModel::sendOtp,
        onFinish = viewModel::completeOnboarding,
    )
}

/** OTP unlock. */
@Composable
private fun Locked(prefs: AppPrefs, viewModel: AppViewModel) {
    OtpLoginScreen(
        phone = prefs.phone.ifBlank { MockValues.DEFAULT_PHONE },
        onVerifyOtp = viewModel::verifyOtp,
        onReset = viewModel::signOutEverywhere,
        demoOtp = BuildConfig.FLAVOR == "mock",
    )
}

/** Locks after 30 s in the background. */
@Composable
private fun LockWhenBackgrounded(onLock: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
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
