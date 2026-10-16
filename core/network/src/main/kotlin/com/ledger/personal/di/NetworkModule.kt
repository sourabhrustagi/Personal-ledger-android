package com.ledger.personal.di

import android.content.Context
import com.ledger.personal.domain.pay.BillPay
import com.ledger.personal.core.TRANSACTION_ATTEMPTS
import com.ledger.personal.data.remote.certificatePinner
import com.ledger.personal.data.remote.api.KtorStaticApi
import com.ledger.personal.data.remote.api.KtorTransactionApi
import com.ledger.personal.data.remote.api.StaticApi
import com.ledger.personal.data.remote.api.TransactionApi
import com.ledger.personal.domain.release.AppRelease
import com.ledger.personal.domain.session.Session
import com.ledger.personal.domain.settings.CategoryCatalog
import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import com.ledger.personal.data.remote.interceptor.StaticCacheInterceptor
import com.ledger.personal.data.remote.interceptor.StaticFreshnessInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.accept
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Hilt bindings for network. */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** Supplies json. */
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    /** Supplies http logging interceptor. */
    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(config: ApiConfig): HttpLoggingInterceptor {
        val interceptor = HttpLoggingInterceptor()
        interceptor.redactHeader("Authorization")
        interceptor.redactHeader("Cookie")
        interceptor.level = if (config.logRequests) {
            HttpLoggingInterceptor.Level.BASIC
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return interceptor
    }

    /** Supplies the authenticated Ktor client. */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideHttpClient(
        config: ApiConfig,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        @ApplicationContext context: Context,
        json: Json,
    ): HttpClient = ktorClient(
        engineClient = finish(timeouts().addInterceptor(authInterceptor), config, context, loggingInterceptor),
        config = config,
        json = json,
        authenticated = true,
    )

    /** Public client for the static endpoints. No Authorization header, and responses are kept on disk. */
    @Provides
    @Singleton
    @StaticClient
    fun provideStaticClient(
        config: ApiConfig,
        loggingInterceptor: HttpLoggingInterceptor,
        @ApplicationContext context: Context,
        json: Json,
    ): HttpClient {
        val engineClient = finish(
            timeouts()
                .cache(Cache(File(context.cacheDir, "static-http"), 2L * 1024 * 1024))
                .addInterceptor(StaticCacheInterceptor())
                .addNetworkInterceptor(StaticFreshnessInterceptor()),
            config,
            context,
            loggingInterceptor,
        )
        return ktorClient(engineClient, config, json)
    }

    /** Same session and pins as the ledger client, without that client's host or write retries. */
    @Provides
    @Singleton
    @TeamClient
    fun provideTeamClient(
        config: ApiConfig,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        @ApplicationContext context: Context,
        json: Json,
    ): HttpClient = ktorClient(
        engineClient = finish(timeouts().addInterceptor(authInterceptor), config, context, loggingInterceptor),
        config = config,
        json = json,
        rooted = false,
    )

    /** Supplies the session. Lock drops the token; unlock posts a new one. */
    @Provides
    @Singleton
    fun provideSession(session: com.ledger.personal.data.remote.api.InterceptorSession): Session = session

    /** Supplies bill pay. Both sides derive the AES key from ECDH; the key is never posted. */
    @Provides
    @Singleton
    fun provideBillPay(pay: com.ledger.personal.data.remote.api.InterceptorBillPay): BillPay = pay

    /** Supplies the transaction API. */
    @Provides
    @Singleton
    fun provideTransactionApi(api: KtorTransactionApi): TransactionApi = api

    /** Supplies the static API. */
    @Provides
    @Singleton
    fun provideStaticApi(api: KtorStaticApi): StaticApi = api

    /** Supplies category catalog. A failed call yields an empty list so a saved catalog is kept. */
    @Provides
    @Singleton
    fun provideCategoryCatalog(api: StaticApi): CategoryCatalog = object : CategoryCatalog {
        override suspend fun names(): List<String> = runCatching { api.categories().categories }.getOrDefault(emptyList())
    }

    /** Supplies app release. A failed call yields null so a downed static API does not lock the app. */
    @Provides
    @Singleton
    fun provideAppRelease(api: StaticApi): AppRelease = object : AppRelease {
        override suspend fun minimumVersionCode(): Int? = runCatching { api.version().versionCode }.getOrNull()
    }

    /**
     * Builds a Ktor client on the hardened OkHttp engine. The authenticated client also speaks WebSockets and
     * retries dropped connections and 5xx answers; the static client relies on its disk cache instead.
     */
    private fun ktorClient(
        engineClient: OkHttpClient,
        config: ApiConfig,
        json: Json,
        authenticated: Boolean = false,
        rooted: Boolean = true,
    ): HttpClient = HttpClient(OkHttp) {
        engine { preconfigured = engineClient }
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
        install(DefaultRequest) {
            if (rooted) url(config.baseUrl)
            accept(ContentType.Application.Json)
        }
        if (authenticated) {
            install(WebSockets)
            install(HttpRequestRetry) {
                retryOnExceptionOrServerErrors(maxRetries = TRANSACTION_ATTEMPTS - 1)
                exponentialDelay()
            }
        }
    }

    /** 30 s so a slow radio can finish a small JSON call before the static cache is used. */
    private fun timeouts(): OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)

    /** Adds logging, debug inspection, and certificate pins, then builds the client. */
    private fun finish(
        builder: OkHttpClient.Builder,
        config: ApiConfig,
        context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
    ): OkHttpClient {
        builder.addInterceptor(loggingInterceptor)
        if (config.debuggable) {
            builder.addInterceptor(
                com.chuckerteam.chucker.api.ChuckerInterceptor.Builder(context)
                    .redactHeaders("Authorization", "Cookie")
                    .build(),
            )
        }
        certificatePinner(config.pinSha256, config.pinBackup)?.let(builder::certificatePinner)
        return builder.build()
    }
}
