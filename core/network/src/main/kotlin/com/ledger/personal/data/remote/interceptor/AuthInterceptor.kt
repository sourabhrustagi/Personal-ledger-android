package com.ledger.personal.data.remote.interceptor

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** OkHttp interceptor for auth. */
@Singleton
class AuthInterceptor @Inject constructor() : Interceptor {

    @Volatile
    private var authToken: String? = null

    /** Updates token. */
    fun updateToken(newToken: String?) {
        this.authToken = newToken
    }

    /** Intercept. */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder().apply {
            authToken?.let { header("Authorization", "Bearer $it") }
        }.build()
        return chain.proceed(request)
    }
}
