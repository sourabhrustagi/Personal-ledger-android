package com.ledger.personal.domain.session

/** Service for requesting and verifying one-time passwords (OTP). */
interface OtpService {
    /** Submits a valid 10-digit mobile number to the server to send an OTP code. */
    suspend fun sendOtp(phone: String): Result<OtpResponse>
}
