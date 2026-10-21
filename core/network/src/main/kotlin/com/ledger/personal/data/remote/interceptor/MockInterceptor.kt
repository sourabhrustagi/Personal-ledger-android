package com.ledger.personal.data.remote.interceptor

import com.ledger.personal.data.remote.mock.MockResponses
import com.ledger.personal.di.ApiConfig
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import javax.inject.Inject
import javax.inject.Singleton

private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"

/**
 * Interceptor for the mock flavor that intercepts HTTP endpoints and simulates
 * backend responses as JSON without requiring live network connectivity.
 */
@Singleton
class MockInterceptor @Inject constructor(
    private val config: ApiConfig,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!config.mockFlavor) {
            return chain.proceed(chain.request())
        }

        val request = chain.request()
        val mockJson = MockResponses.responseFor(request.url.encodedPath, request.method)
        return if (mockJson != null) {
            jsonResponse(chain, mockJson)
        } else {
            chain.proceed(request)
        }
    }

    private fun jsonResponse(chain: Interceptor.Chain, json: String, code: Int = 200): Response =
        Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(json.toResponseBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()
}
