package com.ledger.personal.data.remote.interceptor

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthInterceptorTest {

    @Test
    fun hasNoTokenByDefault() {
        val interceptor = AuthInterceptor()
        val request = intercept(interceptor, Request.Builder().url("http://localhost/transactions").build())
        assertNull(request.header("Content-Type"))
        assertNull(request.header("Authorization"))
    }

    @Test
    fun preservesHeadersAddedByKtor() {
        val interceptor = AuthInterceptor()
        val request = intercept(
            interceptor,
            Request.Builder()
                .url("http://localhost/transactions")
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build(),
        )
        assertNull(request.header("Authorization"))
        assertEquals("application/json", request.header("Accept"))
        assertEquals("application/json", request.header("Content-Type"))
    }

    @Test
    fun usesUpdatedToken() {
        val interceptor = AuthInterceptor()
        interceptor.updateToken("rotated-token")
        val request = intercept(interceptor, Request.Builder().url("http://localhost/transactions").build())
        assertEquals("Bearer rotated-token", request.header("Authorization"))
    }

    private fun intercept(interceptor: AuthInterceptor, original: Request): Request {
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
                    .body("ok".toResponseBody(null))
                    .build()
            }
            .build()
        client.newCall(original).execute().close()
        return captured!!
    }
}
