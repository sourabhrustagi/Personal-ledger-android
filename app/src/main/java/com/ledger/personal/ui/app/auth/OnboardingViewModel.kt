package com.ledger.personal.ui.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.AppConstants
import com.ledger.personal.data.prefs.AppPreferences
import com.ledger.personal.domain.session.OtpResponse
import com.ledger.personal.domain.session.Session
import com.ledger.personal.domain.usecase.SendOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages state and logic for the onboarding flow.
 * Adheres to SRP by handling only onboarding credentials and OTP dispatch.
 */
@HiltViewModel
class OnboardingViewModel internal constructor(
    private val store: AppPreferences?,
    private val sendOtpUseCase: SendOtpUseCase?,
    private val session: Session?,
    @Suppress("UnusedPrivateProperty") private val dummy: Any?,
) : ViewModel() {

    @Inject
    constructor(
        store: AppPreferences,
        sendOtpUseCase: SendOtpUseCase,
        session: Session,
    ) : this(store, sendOtpUseCase, session, null)

    internal constructor(
        sendOtpUseCase: SendOtpUseCase,
    ) : this(null, sendOtpUseCase, null, null)

    private val mutableOtpState = MutableStateFlow(OtpUiState())

    /** Observable UI state for OTP dispatch and status. */
    val otpState: StateFlow<OtpUiState> = mutableOtpState.asStateFlow()

    private val mutableResendCountdown = MutableStateFlow(0)

    /** Observable countdown timer in seconds before OTP resend is allowed. Survives configuration changes. */
    val resendCountdown: StateFlow<Int> = mutableResendCountdown.asStateFlow()

    private var timerJob: Job? = null

    /** Starts the resend countdown ticking from [durationSeconds] down to 0. */
    fun startResendTimer(durationSeconds: Int = RESEND_SECONDS) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (i in durationSeconds downTo 0) {
                mutableResendCountdown.value = i
                mutableOtpState.update { it.copy(resendCountdown = i) }
                if (i > 0) {
                    delay(ONE_SECOND_MS)
                }
            }
        }
    }

    /** Submits valid phone number through use case to send an OTP code. */
    fun sendOtp(phone: String) {
        viewModelScope.launch {
            mutableOtpState.update { it.copy(isSending = true, errorMessage = null) }
            val result = sendOtpUseCase?.invoke(phone)
                ?: Result.success(OtpResponse(message = "OTP sent"))
            val isSuccess = result.isSuccess
            mutableOtpState.update {
                it.copy(
                    isSending = false,
                    isOtpSent = isSuccess,
                    sentMessage = result.getOrNull()?.message?.ifBlank { "OTP sent" } ?: "OTP sent",
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
            if (isSuccess) {
                startResendTimer()
            }
        }
    }

    /** Finishes onboarding with mobile number, opens a session, and notifies on completion. */
    fun completeOnboarding(phone: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            store?.completeOnboarding(phone)
            session?.begin()
            onFinished()
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        /** Cooldown duration in seconds before OTP resend is permitted. */
        const val RESEND_SECONDS: Int = AppConstants.OTP_RESEND_COOLDOWN_SECONDS
        private const val ONE_SECOND_MS: Long = 1_000L
    }
}
