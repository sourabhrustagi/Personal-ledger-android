package com.ledger.personal.data.remote.interceptor

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

class StaticCallTest {
    @Test
    fun staticCallsDropTheSessionAndFallBackToCache() {
        var calls = 0
        var retried: Request? = null
        val client = OkHttpClient.Builder()
            .addInterceptor(StaticCacheInterceptor())
            .addInterceptor { chain ->
                calls += 1
                if (calls == 1) throw IOException("offline")
                retried = chain.request()
                ok(chain.request())
            }
            .build()
        client.newCall(
            Request.Builder()
                .url("http://localhost/static/version")
                .header("Authorization", "Bearer secret")
                .header("Content-Type", "application/json")
                .build(),
        ).execute().close()
        val request = requireNotNull(retried)
        assertEquals("application/json", request.header("Accept"))
        assertNull(request.header("Authorization"))
        assertNull(request.header("Content-Type"))
        assertEquals("public, only-if-cached, max-stale=604800", request.header("Cache-Control"))
    }

    private fun ok(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("ok".toResponseBody(null))
        .build()
}
