package com.ledger.personal.data.remote.interceptor

import com.ledger.personal.data.remote.BankingHeaders
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BankingHeaderInterceptorTest {

    @Test
    fun injectsMandatoryBankingHeadersOnGetRequest() {
        val interceptor = BankingHeaderInterceptor()
        val request = intercept(
            interceptor,
            Request.Builder().url("https://api.personal-ledger.com/retail/v1/accounts").build(),
        )

        assertEquals(BankingHeaders.APPLICATION_JSON, request.header(BankingHeaders.ACCEPT))
        assertEquals(BankingHeaders.PLATFORM_ANDROID, request.header(BankingHeaders.X_PLATFORM))
        assertEquals(BankingHeaders.CHANNEL_MOBILE_BANKING, request.header(BankingHeaders.X_CHANNEL))
        assertEquals("1.0.0", request.header(BankingHeaders.X_APP_VERSION))

        // Distributed tracing
        val correlationId = request.header(BankingHeaders.X_CORRELATION_ID)
        assertNotNull(correlationId)
        UUID.fromString(correlationId) // valid UUID

        val requestId = request.header(BankingHeaders.X_REQUEST_ID)
        assertNotNull(requestId)
        UUID.fromString(requestId)

        // Timestamp
        val timestamp = request.header(BankingHeaders.X_TIMESTAMP)
        assertNotNull(timestamp)
        assertTrue(timestamp!!.toLong() > 0)

        // User agent
        val userAgent = request.header(BankingHeaders.USER_AGENT)
        assertNotNull(userAgent)
        assertTrue(userAgent!!.startsWith("PersonalLedger-Android/"))

        // GET requests should not receive an automatic idempotency key
        assertNull(request.header(BankingHeaders.X_IDEMPOTENCY_KEY))
    }

    @Test
    fun injectsIdempotencyKeyOnMutatingPostRequest() {
        val interceptor = BankingHeaderInterceptor()
        val request = intercept(
            interceptor,
            Request.Builder()
                .url("https://api.personal-ledger.com/retail/v1/transfers")
                .post("{\"amount\": 1000}".toRequestBody(null))
                .build(),
        )

        val idempotencyKey = request.header(BankingHeaders.X_IDEMPOTENCY_KEY)
        assertNotNull(idempotencyKey)
        UUID.fromString(idempotencyKey)
    }

    @Test
    fun preservesCallerSuppliedCorrelationIdAndIdempotencyKey() {
        val customCorrelation = "corr-test-12345"
        val customIdempotency = "idem-test-67890"

        val interceptor = BankingHeaderInterceptor()
        val request = intercept(
            interceptor,
            Request.Builder()
                .url("https://api.personal-ledger.com/retail/v1/transfers")
                .header(BankingHeaders.X_CORRELATION_ID, customCorrelation)
                .header(BankingHeaders.X_IDEMPOTENCY_KEY, customIdempotency)
                .post("{}".toRequestBody(null))
                .build(),
        )

        assertEquals(customCorrelation, request.header(BankingHeaders.X_CORRELATION_ID))
        assertEquals(customIdempotency, request.header(BankingHeaders.X_IDEMPOTENCY_KEY))
    }

    @Test
    fun preservesCustomAcceptAndUserAgent() {
        val customAccept = "application/vnd.banking.v2+json"
        val customUserAgent = "PartnerFintechClient/3.1.0"

        val interceptor = BankingHeaderInterceptor()
        val request = intercept(
            interceptor,
            Request.Builder()
                .url("https://api.personal-ledger.com/retail/v1/accounts")
                .header(BankingHeaders.ACCEPT, customAccept)
                .header(BankingHeaders.USER_AGENT, customUserAgent)
                .build(),
        )

        assertEquals(customAccept, request.header(BankingHeaders.ACCEPT))
        assertEquals(customUserAgent, request.header(BankingHeaders.USER_AGENT))
    }

    private fun intercept(interceptor: BankingHeaderInterceptor, original: Request): Request {
        var captured: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                captured = chain.request()
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("{}".toResponseBody(null))
                    .build()
            }
            .build()
        client.newCall(original).execute().close()
        return requireNotNull(captured)
    }
}
