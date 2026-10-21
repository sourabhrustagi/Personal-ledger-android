package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import com.ledger.personal.di.ApiConfig
import com.ledger.personal.di.StaticClient
import com.ledger.personal.domain.session.Session
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
data class SessionDto(val token: String = "")

@Serializable
data class OtpRequestDto(val phone: String)

@Serializable
data class OtpResponseDto(val message: String = "OTP sent", val status: String = "SENT")

const val MOCK_SESSION_TOKEN = "mock-session"
const val MOCK_OTP_SENT_MESSAGE = "OTP sent"

private const val PHONE_LENGTH = 10

/** Posts a session on the public client, or issues a local token on the mock flavor. */
@Singleton
class InterceptorSession @Inject constructor(
    private val auth: AuthInterceptor,
    private val config: ApiConfig,
    @param:StaticClient private val client: HttpClient,
) : Session {
    override fun end() = auth.updateToken(null)

    override suspend fun begin(): Result<Unit> = runCatching {
        val token = if (config.mockFlavor) MOCK_SESSION_TOKEN else client.post("session").body<SessionDto>().token
        require(token.isNotBlank()) { "Empty session" }
        auth.updateToken(token)
    }.onFailure { if (it is CancellationException) throw it }

    override suspend fun sendOtp(phone: String): Result<String> = runCatching {
        require(phone.length == PHONE_LENGTH && phone.all(Char::isDigit)) { "Invalid 10-digit phone number" }
        if (config.mockFlavor) {
            MOCK_OTP_SENT_MESSAGE
        } else {
            val response = client.post("auth/otp") {
                contentType(ContentType.Application.Json)
                setBody(OtpRequestDto(phone))
            }.body<OtpResponseDto>()
            response.message.ifBlank { MOCK_OTP_SENT_MESSAGE }
        }
    }.onFailure { if (it is CancellationException) throw it }
}
