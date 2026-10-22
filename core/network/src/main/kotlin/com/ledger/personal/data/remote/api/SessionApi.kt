package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import com.ledger.personal.domain.session.Session
import com.ledger.personal.domain.session.SessionResponse
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@OptIn(InternalSerializationApi::class)
@Serializable
data class SessionDto(val token: String = "") {
    fun toDomain(): SessionResponse = SessionResponse(token)
}

const val MOCK_SESSION_TOKEN = "mock-session"

/** Retrofit API contract for session authentication. */
interface SessionRetrofitApi {
    @POST(ApiEndpoints.SESSION)
    suspend fun begin(): SessionDto
}

/** Posts a session on the static client via Retrofit and stores the token into [auth]. */
@Singleton
class InterceptorSession @Inject constructor(
    private val auth: AuthInterceptor,
    private val sessionApi: SessionRetrofitApi,
) : Session {
    override fun end() = auth.updateToken(null)

    override suspend fun begin(): Result<SessionResponse> = runCatching {
        val dto = sessionApi.begin()
        require(dto.token.isNotBlank()) { "Empty session" }
        auth.updateToken(dto.token)
        dto.toDomain()
    }.onFailure { if (it is CancellationException) throw it }
}

