package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import com.ledger.personal.di.ApiConfig
import com.ledger.personal.di.StaticClient
import com.ledger.personal.domain.session.Session
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class SessionDto(val token: String = "")

const val MOCK_SESSION_TOKEN = "mock-session"

/** Posts a session on the public client, or issues a local token on the mock flavor. */
@Singleton
class InterceptorSession @Inject constructor(
    private val auth: AuthInterceptor,
    private val config: ApiConfig,
    @param:StaticClient private val client: HttpClient,
) : Session {
    override fun end() = auth.updateToken(null)

    override suspend fun begin(): Result<Unit> = runCatching {
        val token = if (config.localSession) MOCK_SESSION_TOKEN else client.post("session").body<SessionDto>().token
        require(token.isNotBlank()) { "Empty session" }
        auth.updateToken(token)
    }.onFailure { if (it is CancellationException) throw it }
}
