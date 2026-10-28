package com.ledger.personal.feature.auth

/**
 * Global application constants for UI timings, limits, and authentication flows.
 */
object AppConstants {
    /** Cooldown duration in seconds before an OTP resend request is permitted. */
    const val OTP_RESEND_COOLDOWN_SECONDS: Int = 30

    /** Standard OTP code character length for SMS verification. */
    const val OTP_CODE_LENGTH: Int = 6
}
