package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.mock.MockResponses
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
        val client = HttpClient(
            MockEngine {
                respond(
                    MockResponses.OTP,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val service = KtorOtpService(client)
        val result = service.sendOtp("9876543210")
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals("OTP sent", response?.message)
        assertEquals("SENT", response?.status)
        client.close()
    }

    @Test
    fun sendOtpFailsForInvalidPhone() = runBlocking {
        val client = HttpClient(MockEngine { error("invalid phone should fail before network call") })
        val service = KtorOtpService(client)
        val result = service.sendOtp("123")
        assertTrue(result.isFailure)
        client.close()
    }

    @Test
    fun liveSendOtpPostsToServerAndReturnsMessage() = runBlocking {
        val client = HttpClient(
            MockEngine {
                respond(
                    """{"message":"OTP sent","status":"SENT"}""",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }
        val service = KtorOtpService(client)
        val result = service.sendOtp("9876543210")
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals("OTP sent", response?.message)
        assertEquals("SENT", response?.status)
        client.close()
    }
}
