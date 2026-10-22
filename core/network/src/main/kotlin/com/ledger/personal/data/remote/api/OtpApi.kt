package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.data.remote.safeApiCall
import com.ledger.personal.di.IoDispatcher
import com.ledger.personal.domain.session.OtpRepository
import com.ledger.personal.domain.session.OtpResponse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OtpRequestDto(val phone: String)

@Serializable
data class OtpResponseDto(
    val message: String = "OTP sent",
    val status: String = "SUCCESS",
    val verificationToken: String? = null,
) {
    fun toDomain(): OtpResponse = OtpResponse(
        message = message.ifBlank { MOCK_OTP_SENT_MESSAGE },
        status = status,
        verificationToken = verificationToken,
    )
}

const val MOCK_OTP_SENT_MESSAGE = "OTP sent"
private const val PHONE_LENGTH = 10

/** Retrofit API contract for phone OTP verification codes. */
interface OtpRetrofitApi {
    @POST(ApiEndpoints.OTP)
    suspend fun sendOtp(@Body request: OtpRequestDto): OtpResponseDto
}

/**
 * Retrofit-backed repository for requesting phone OTP verification codes.
 * Simplified using safeApiCall to automatically ensure main-safety and exception propagation.
 */
@Singleton
class RetrofitOtpRepository @Inject constructor(
    private val api: OtpRetrofitApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : OtpRepository {
    override suspend fun sendOtp(phone: String): Result<OtpResponse> = safeApiCall(ioDispatcher) {
        require(phone.length == PHONE_LENGTH && phone.all(Char::isDigit)) { "Invalid 10-digit phone number" }
        api.sendOtp(OtpRequestDto(phone)).toDomain()
    }
}

/** Backward-compatible typealias for [RetrofitOtpRepository]. */
typealias RetrofitOtpService = RetrofitOtpRepository

/** Backward-compatible typealias for [RetrofitOtpRepository]. */
typealias KtorOtpService = RetrofitOtpRepository
