package com.ledger.personal.data.remote.interceptor

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.data.remote.api.MOCK_SESSION_TOKEN
import com.ledger.personal.di.ApiConfig
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockInterceptorTest {

    @Test
    fun mockFlavorInterceptsSessionEndpoint() {
        val interceptor = MockInterceptor(createConfig(mockFlavor = true))
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.SESSION}")
                .post("{}".toRequestBody(null))
                .build(),
        ).execute()

        assertEquals(200, response.code)
        val body = response.body?.string().orEmpty()
        assertTrue(body.contains(MOCK_SESSION_TOKEN))
        response.close()
    }

    @Test
    fun mockFlavorInterceptsOtpEndpoint() {
        val interceptor = MockInterceptor(createConfig(mockFlavor = true))
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.OTP}")
                .post("{}".toRequestBody(null))
                .build(),
        ).execute()

        assertEquals(200, response.code)
        val body = response.body?.string().orEmpty()
        assertTrue(body.contains("OTP sent"))
        response.close()
    }

    @Test
    fun mockFlavorInterceptsTransactionsEndpoint() {
        val interceptor = MockInterceptor(createConfig(mockFlavor = true))
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.TRANSACTIONS}")
                .get()
                .build(),
        ).execute()

        assertEquals(200, response.code)
        val body = response.body?.string().orEmpty()
        assertTrue(body.contains("items"))
        assertTrue(body.contains("Starbucks Coffee"))
        response.close()
    }

    @Test
    fun mockFlavorInterceptsDashboardEndpoint() {
        val interceptor = MockInterceptor(createConfig(mockFlavor = true))
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.DASHBOARD}")
                .get()
                .build(),
        ).execute()

        assertEquals(200, response.code)
        val body = response.body?.string().orEmpty()
        assertTrue(body.contains("tabs"))
        assertTrue(body.contains("sections"))
        response.close()
    }

    @Test
    fun mockFlavorInterceptsStaticCategoriesEndpoint() {
        val interceptor = MockInterceptor(createConfig(mockFlavor = true))
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.STATIC_CATEGORIES}")
                .get()
                .build(),
        ).execute()

        assertEquals(200, response.code)
        val body = response.body?.string().orEmpty()
        assertTrue(body.contains("categories"))
        assertTrue(body.contains("Dining"))
        response.close()
    }

    @Test
    fun mockFlavorInterceptsDeleteTransactionEndpoint() {
        val interceptor = MockInterceptor(createConfig(mockFlavor = true))
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.TRANSACTIONS}/tx-mock-1")
                .delete()
                .build(),
        ).execute()

        assertEquals(200, response.code)
        val body = response.body?.string().orEmpty()
        assertTrue(body.contains("success"))
        response.close()
    }

    @Test
    fun liveFlavorPassesThroughToChain() {
        var chainProceedCalled = false
        val interceptor = MockInterceptor(createConfig(mockFlavor = false))
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .addInterceptor { chain ->
                chainProceedCalled = true
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("live-response".toResponseBody(null))
                    .build()
            }
            .build()

        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.SESSION}")
                .post("{}".toRequestBody(null))
                .build(),
        ).execute()

        assertTrue(chainProceedCalled)
        assertEquals("live-response", response.body?.string())
        response.close()
    }

    @Test
    fun mockFlavorSleepsDuringInterception() {
        val interceptor = MockInterceptor(createConfig(mockFlavor = true), delayMs = 100L)
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        val startTime = System.currentTimeMillis()
        val response = client.newCall(
            Request.Builder()
                .url("https://api.example/retail/v1/${ApiEndpoints.SESSION}")
                .post("{}".toRequestBody(null))
                .build(),
        ).execute()

        val elapsed = System.currentTimeMillis() - startTime
        assertTrue("Elapsed should be at least 100ms, was $elapsed ms", elapsed >= 90)
        assertEquals(200, response.code)
        response.close()
    }

    private fun createConfig(mockFlavor: Boolean) = ApiConfig(
        baseUrl = "https://api.example/retail/v1/",
        logRequests = false,
        debuggable = false,
        pinSha256 = "",
        pinBackup = "",
        paymentsBaseUrl = "",
        promotionsBaseUrl = "",
        mockFlavor = mockFlavor,
    )
}
