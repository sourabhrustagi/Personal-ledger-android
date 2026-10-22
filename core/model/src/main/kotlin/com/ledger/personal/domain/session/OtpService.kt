package com.ledger.personal.domain.session

/** Repository for requesting and verifying one-time passwords (OTP). */
interface OtpRepository {
    /** Submits a valid 10-digit mobile number to the server to send an OTP code. */
    suspend fun sendOtp(phone: String): Result<OtpResponse>
}

/** Backward-compatible alias for [OtpRepository]. */
typealias OtpService = OtpRepository
