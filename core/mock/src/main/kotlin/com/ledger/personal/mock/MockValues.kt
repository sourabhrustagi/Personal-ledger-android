package com.ledger.personal.mock

/**
 * Central mock values used across demo and test workflows.
 * Edit these constants to change the mock behavior and demo credentials across the project.
 */
object MockValues {
    /** 6-digit mock OTP for onboarding SMS verification. */
    const val OTP: String = "123456"

    /** 4-digit demo MPIN accepted by the mock build flavor. */
    const val MPIN: String = "1111"

    /** Default mock user display name. */
    const val DEFAULT_NAME: String = "Asha Rao"

    /** Default mock mobile phone number (without country code). */
    const val DEFAULT_PHONE: String = "9876543210"

    /** Default mock session token for local/mock API authentication. */
    const val SESSION_TOKEN: String = "mock-session"
}
