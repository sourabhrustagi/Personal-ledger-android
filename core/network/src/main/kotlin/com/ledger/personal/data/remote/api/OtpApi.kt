package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.di.StaticClient
import com.ledger.personal.domain.session.OtpResponse
import com.ledger.personal.domain.session.OtpService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

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

/** Ktor-backed service for requesting phone OTP verification codes. */
@Singleton
class KtorOtpService @Inject constructor(
    @param:StaticClient private val client: HttpClient,
) : OtpService {
    override suspend fun sendOtp(phone: String): Result<OtpResponse> = runCatching {
        require(phone.length == PHONE_LENGTH && phone.all(Char::isDigit)) { "Invalid 10-digit phone number" }
        val response = client.post(ApiEndpoints.OTP) {
            contentType(ContentType.Application.Json)
            setBody(OtpRequestDto(phone))
        }.body<OtpResponseDto>()
        response.toDomain()
    }.onFailure { if (it is CancellationException) throw it }
}
