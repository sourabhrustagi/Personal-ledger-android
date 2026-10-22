package com.ledger.personal.domain.usecase

import com.ledger.personal.domain.session.OtpResponse
import com.ledger.personal.domain.session.OtpService
import javax.inject.Inject

private const val MIN_PHONE_DIGITS = 10

/**
 * Use case to validate phone and request an OTP.
 */
class SendOtpUseCase @Inject constructor(
    private val otpService: OtpService,
) {
    suspend operator fun invoke(phone: String): Result<OtpResponse> {
        val digits = phone.filter { it.isDigit() }
        if (digits.length < MIN_PHONE_DIGITS) {
            return Result.failure(IllegalArgumentException("Please enter a valid 10-digit mobile number"))
        }
        return otpService.sendOtp(phone)
    }
}
