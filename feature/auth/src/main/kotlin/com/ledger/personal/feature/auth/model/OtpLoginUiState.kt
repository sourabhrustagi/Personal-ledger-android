package com.ledger.personal.feature.auth.model

/** State for OTP login screen. */
data class OtpLoginUiState(
    val phone: String,
    val demoOtp: Boolean = false,
)
