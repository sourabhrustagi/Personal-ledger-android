package com.ledger.personal.ui.app.auth

/** OTP request and verification state. */
data class OtpUiState(
    val isSending: Boolean = false,
    val isOtpSent: Boolean = false,
    val isOtpError: Boolean = false,
    val sentMessage: String = "OTP sent",
    val errorMessage: String? = null,
    val resendCountdown: Int = 0,
    val phone: String = "",
    val otpCode: String = "",
)
