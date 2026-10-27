package com.ledger.personal.ui.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.AppConstants
import com.ledger.personal.data.prefs.AppPreferences
import com.ledger.personal.data.security.DeviceKeys
import com.ledger.personal.domain.session.Session
import com.ledger.personal.mock.MockValues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages unlock verification and account reset for the locked/login screen.
 * Adheres to SRP by handling only user unlock authentication and device reset.
 */
@HiltViewModel
class LoginViewModel internal constructor(
    private val store: AppPreferences?,
    private val keys: DeviceKeys?,
    private val session: Session?,
    @Suppress("UnusedPrivateProperty") private val dummy: Any?,
) : ViewModel() {

    @Inject
    constructor(
        store: AppPreferences,
        keys: DeviceKeys,
        session: Session,
    ) : this(store, keys, session, null)

    internal constructor() : this(null, null, null, null)

    private val mutableResendCountdown = MutableStateFlow(AppConstants.OTP_RESEND_COOLDOWN_SECONDS)

    /** Observable countdown timer in seconds before OTP resend is allowed. Survives configuration changes. */
    val resendCountdown: StateFlow<Int> = mutableResendCountdown.asStateFlow()

    private var timerJob: Job? = null

    init {
        startResendTimer()
    }

    /** Starts the resend countdown ticking from [durationSeconds] down to 0. */
    fun startResendTimer(durationSeconds: Int = AppConstants.OTP_RESEND_COOLDOWN_SECONDS) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in durationSeconds downTo 0) {
                mutableResendCountdown.value = i
                if (i > 0) {
                    delay(ONE_SECOND_MS)
                }
            }
        }
    }

    /** Verifies OTP to unlock / sign in. [onResult] runs after session is opened. */
    fun verifyOtp(code: String, onResult: (Boolean) -> Unit) {
        if (code != MockValues.OTP) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = session?.begin() ?: Result.success(com.ledger.personal.domain.session.SessionResponse("test_token"))
            onResult(result.isSuccess)
        }
    }

    /** Clears session tokens, keystore keys, and local preferences to reset the app. */
    fun signOutEverywhere(onResetDone: () -> Unit = {}) {
        viewModelScope.launch {
            session?.end()
            keys?.delete()
            store?.reset()
            onResetDone()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private const val ONE_SECOND_MS: Long = 1_000L
    }
}
