package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import com.ledger.personal.di.StaticClient
import com.ledger.personal.domain.session.Session
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@OptIn(InternalSerializationApi::class)
@Serializable
data class SessionDto(val token: String = "")

const val MOCK_SESSION_TOKEN = "mock-session"

/** Posts a session on the public client and stores the token into [auth]. */
@Singleton
class InterceptorSession @Inject constructor(
    private val auth: AuthInterceptor,
    @param:StaticClient private val client: HttpClient,
) : Session {
    override fun end() = auth.updateToken(null)

    override suspend fun begin(): Result<Unit> = runCatching {
        val token = client.post("session").body<SessionDto>().token
        require(token.isNotBlank()) { "Empty session" }
        auth.updateToken(token)
    }.onFailure { if (it is CancellationException) throw it }
}
