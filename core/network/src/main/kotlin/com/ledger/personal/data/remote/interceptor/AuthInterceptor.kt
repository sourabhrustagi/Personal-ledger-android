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
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()

        authToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}
