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
import com.ledger.personal.ui.app.auth.LoginViewModel
import com.ledger.personal.ui.app.auth.OnboardingFlow
import com.ledger.personal.ui.app.auth.OnboardingViewModel
import com.ledger.personal.ui.app.auth.OtpLoginScreen
import com.ledger.personal.ui.app.auth.OtpLoginUiState
import com.ledger.personal.ui.app.update.ForceUpdateScreen
import com.ledger.personal.ui.app.update.ForceUpdateUiState
import com.ledger.personal.ui.app.update.SoftUpdateDialog
import com.ledger.personal.ui.navigation.LedgerNavHost
import com.ledger.personal.ui.theme.LedgerOneTheme
import com.ledger.personal.ui.theme.ThemeMode


/** Picks splash, update, onboarding, lock, or the signed-in app. */
@Composable
fun LedgerOneApp(viewModel: AppViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    LockWhenBackgrounded(viewModel::lock)
    LedgerOneTheme(prefs?.theme ?: ThemeMode.SYSTEM) {
        when (val screen = status) {
            AppStatus.Loading -> Surface(Modifier.fillMaxSize()) {}
            AppStatus.Update -> ForceUpdateScreen(ForceUpdateUiState(BuildConfig.VERSION_NAME))
            is AppStatus.Onboard -> Onboard(onOnboarded = viewModel::unlock)
            is AppStatus.Locked -> Locked(
                prefs = screen.prefs,
                onUnlocked = viewModel::unlock,
                onReset = viewModel::resetSession,
            )
            is AppStatus.Personal -> LedgerNavHost(screen.prefs, viewModel)
        }
        if (status.softUpdate) {
            SoftUpdateDialog(onUpdate = viewModel::dismissUpdateOffer, onLater = viewModel::ignoreUpdate)
        }
    }
}

/** Runs onboarding with phone and OTP using dedicated OnboardingViewModel. */
@Composable
private fun Onboard(
    onOnboarded: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val otpState by viewModel.otpState.collectAsStateWithLifecycle()
    OnboardingFlow(
        otpState = otpState,
        onSendOtp = viewModel::sendOtp,
        onFinish = { phone ->
            viewModel.completeOnboarding(phone, onOnboarded)
        },
    )
}

/** OTP unlock using dedicated LoginViewModel. */
@Composable
private fun Locked(
    prefs: AppPrefs,
    onUnlocked: () -> Unit,
    onReset: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    OtpLoginScreen(
        uiState = OtpLoginUiState(
            phone = prefs.phone.ifBlank { MockValues.DEFAULT_PHONE },
            demoOtp = BuildConfig.FLAVOR == "mock",
        ),
        onVerifyOtp = { code, onResult ->
            viewModel.verifyOtp(code) { success ->
                onResult(success)
                if (success) onUnlocked()
            }
        },
        onReset = {
            viewModel.signOutEverywhere(onReset)
        },
    )
}

/** Locks immediately when backgrounded and returning. */
@Composable
private fun LockWhenBackgrounded(onLock: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val activity = LocalActivity.current
    DisposableEffect(owner) {
        val observer = createBackgroundLockObserver(
            isChangingConfigurations = { activity?.isChangingConfigurations == true },
            onLock = onLock,
        )
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

internal fun createBackgroundLockObserver(
    isChangingConfigurations: () -> Boolean,
    onLock: () -> Unit,
): LifecycleEventObserver {
    var wasStopped = false
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_STOP -> {
                if (!isChangingConfigurations()) {
                    wasStopped = true
                    onLock()
                }
            }
            Lifecycle.Event.ON_START -> {
                if (wasStopped) {
                    wasStopped = false
                    onLock()
                }
            }
            else -> Unit
        }
    }
}
