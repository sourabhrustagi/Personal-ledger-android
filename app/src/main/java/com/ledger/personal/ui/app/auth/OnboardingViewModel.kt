package com.ledger.personal.ui.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.data.prefs.AppPreferences
import com.ledger.personal.domain.session.Session
import com.ledger.personal.domain.usecase.SendOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
class OnboardingViewModel @Inject constructor(
    private val store: AppPreferences,
    private val sendOtpUseCase: SendOtpUseCase,
    private val session: Session,
) : ViewModel() {

    private val mutableOtpState = MutableStateFlow(OtpUiState())

    /** Observable UI state for OTP dispatch and status. */
    val otpState: StateFlow<OtpUiState> = mutableOtpState.asStateFlow()

    /** Submits valid phone number through use case to send an OTP code. */
    fun sendOtp(phone: String) {
        viewModelScope.launch {
            mutableOtpState.update { it.copy(isSending = true, errorMessage = null) }
            val result = sendOtpUseCase(phone)
            mutableOtpState.update {
                it.copy(
                    isSending = false,
                    isOtpSent = result.isSuccess,
                    sentMessage = result.getOrNull()?.message?.ifBlank { "OTP sent" } ?: "OTP sent",
                    errorMessage = result.exceptionOrNull()?.message,
                )
            }
        }
    }

    /** Finishes onboarding with mobile number, opens a session, and notifies on completion. */
    fun completeOnboarding(phone: String, onFinished: () -> Unit = {}) {
        viewModelScope.launch {
            store.completeOnboarding(phone)
            session.begin()
            onFinished()
        }
    }
}
