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
private const val DEFAULT_MOCK_DELAY_MS = 2_000L

/**
 * Interceptor for the mock flavor that intercepts HTTP endpoints and simulates
 * backend responses as JSON without requiring live network connectivity.
 */
@Singleton
class MockInterceptor @Inject constructor(
    private val config: ApiConfig,
) : Interceptor {

    private var delayMs: Long = DEFAULT_MOCK_DELAY_MS

    constructor(config: ApiConfig, delayMs: Long) : this(config) {
        this.delayMs = delayMs
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!config.mockFlavor) {
            return chain.proceed(chain.request())
        }

        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
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
