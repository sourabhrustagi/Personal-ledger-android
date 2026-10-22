package com.ledger.personal.data.remote.interceptor

import com.ledger.personal.data.remote.BankingHeaders
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

private const val MAX_AGE_SECONDS = 86_400
private const val MAX_STALE_SECONDS = 604_800

/**
 * Static GETs carry no session.
 * When the network fails, the same call is served from the disk cache for up to a week.
 */
class StaticCacheInterceptor : Interceptor {
    /** Intercept. */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header(BankingHeaders.ACCEPT, BankingHeaders.APPLICATION_JSON)
            .removeHeader(BankingHeaders.AUTHORIZATION)
            .removeHeader(BankingHeaders.CONTENT_TYPE)
            .build()
        return try {
            chain.proceed(request)
        } catch (_: IOException) {
            chain.proceed(
                request.newBuilder()
                    .header(BankingHeaders.CACHE_CONTROL, "public, only-if-cached, max-stale=$MAX_STALE_SECONDS")
                    .build(),
            )
        }
    }
}

/**
 * Lets OkHttp store a static response when the server sends no Cache-Control.
 * ponytail: a response that must not be stored has to say so; otherwise it is kept for a day.
 */
class StaticFreshnessInterceptor : Interceptor {
    /** Intercept. */
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.header(BankingHeaders.CACHE_CONTROL) != null) return response
        return response.newBuilder().header(BankingHeaders.CACHE_CONTROL, "public, max-age=$MAX_AGE_SECONDS").build()
    }
}
