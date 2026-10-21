package com.ledger.personal.data.remote.api

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpApiTest {
    @Test
    fun mockSendOtpReturnsOtpSentForValidPhone() = runBlocking {
        val service = KtorOtpService(config(mockFlavor = true), unusedClient())
        val result = service.sendOtp("9876543210")
        assertTrue(result.isSuccess)
        assertEquals("OTP sent", result.getOrNull())
    }

    @Test
    fun sendOtpFailsForInvalidPhone() = runBlocking {
        val service = KtorOtpService(config(mockFlavor = true), unusedClient())
        val result = service.sendOtp("123")
        assertTrue(result.isFailure)
    }

    @Test
    fun liveSendOtpPostsToServerAndReturnsMessage() = runBlocking {
        val client = HttpClient(MockEngine { respond("""{"message":"OTP sent","status":"SENT"}""", headers = headersOf(HttpHeaders.ContentType, "application/json")) }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val service = KtorOtpService(config(mockFlavor = false), client)
        val result = service.sendOtp("9876543210")
        assertTrue(result.isSuccess)
        assertEquals("OTP sent", result.getOrNull())
        client.close()
    }

    private fun config(mockFlavor: Boolean) = ApiConfig(
        baseUrl = "https://api.example/retail/v1/",
        logRequests = false,
        debuggable = false,
        pinSha256 = "",
        pinBackup = "",
        paymentsBaseUrl = "",
        promotionsBaseUrl = "",
        mockFlavor = mockFlavor,
    )

    private fun unusedClient() = HttpClient(MockEngine { error("mock OTP service must not call the network") })
}
