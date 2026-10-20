package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import com.ledger.personal.di.ApiConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InterceptorSessionTest {
    @Test
    fun mockBeginSetsALocalTokenAndEndClearsIt() = runBlocking {
        val auth = AuthInterceptor()
        val session = InterceptorSession(auth, config(local = true), unusedClient())
        assertTrue(session.begin().isSuccess)
        assertEquals("Bearer $MOCK_SESSION_TOKEN", bearer(auth))
        session.end()
        assertNull(bearer(auth))
    }

    @Test
    fun liveBeginStoresTheServerToken() = runBlocking {
        val auth = AuthInterceptor()
        val client = HttpClient(MockEngine { respond("""{"token":"live-token"}""", headers = headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val session = InterceptorSession(auth, config(local = false), client)
        assertTrue(session.begin().isSuccess)
        assertEquals("Bearer live-token", bearer(auth))
        client.close()
    }

    private fun config(local: Boolean) = ApiConfig(
        baseUrl = "https://api.example/retail/v1/",
        logRequests = false,
        debuggable = false,
        pinSha256 = "",
        pinBackup = "",
        paymentsBaseUrl = "",
        promotionsBaseUrl = "",
        localSession = local,
    )

    private fun unusedClient() = HttpClient(MockEngine { error("mock session must not call the network") })

    private fun bearer(auth: AuthInterceptor): String? {
        var header: String? = null
        OkHttpClient.Builder()
            .addInterceptor(auth)
            .addInterceptor { chain ->
                header = chain.request().header("Authorization")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("ok".toResponseBody(null))
                    .build()
            }
            .build()
            .newCall(Request.Builder().url("http://localhost/session").build())
            .execute()
            .close()
        return header
    }
}
