package com.ledger.personal.domain.session

/**
 * Domain model representing the response from requesting an OTP code.
 */
data class OtpResponse(
    val message: String = "OTP sent",
    val status: String = "SUCCESS",
    val verificationToken: String? = null,
)
