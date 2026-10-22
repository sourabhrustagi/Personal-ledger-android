package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import kotlinx.coroutines.runBlocking
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
        val sessionApi = object : SessionRetrofitApi {
            override suspend fun begin(): SessionDto = SessionDto(MOCK_SESSION_TOKEN)
        }
        val session = InterceptorSession(auth, sessionApi)
        val result = session.begin()
        assertTrue(result.isSuccess)
        assertEquals(MOCK_SESSION_TOKEN, result.getOrNull()?.token)
        assertEquals("Bearer $MOCK_SESSION_TOKEN", bearer(auth))
        session.end()
        assertNull(bearer(auth))
    }

    @Test
    fun liveBeginStoresTheServerToken() = runBlocking {
        val auth = AuthInterceptor()
        val sessionApi = object : SessionRetrofitApi {
            override suspend fun begin(): SessionDto = SessionDto("live-token")
        }
        val session = InterceptorSession(auth, sessionApi)
        val result = session.begin()
        assertTrue(result.isSuccess)
        assertEquals("live-token", result.getOrNull()?.token)
        assertEquals("Bearer live-token", bearer(auth))
    }

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
