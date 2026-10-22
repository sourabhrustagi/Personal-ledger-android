package com.ledger.personal.di

import android.content.Context
import com.ledger.personal.core.TRANSACTION_ATTEMPTS
import com.ledger.personal.data.remote.api.BillPayRetrofitApi
import com.ledger.personal.data.remote.api.DashboardRetrofitApi
import com.ledger.personal.data.remote.api.InterceptorBillPay
import com.ledger.personal.data.remote.api.InterceptorSession
import com.ledger.personal.data.remote.api.OtpRetrofitApi
import com.ledger.personal.data.remote.api.PaymentsRetrofitApi
import com.ledger.personal.data.remote.api.PromotionsRetrofitApi
import com.ledger.personal.data.remote.api.RetrofitOtpRepository
import com.ledger.personal.data.remote.api.RetrofitOtpService
import com.ledger.personal.data.remote.api.RetrofitTransactionNetworkDataSource
import com.ledger.personal.data.remote.api.SessionRetrofitApi
import com.ledger.personal.data.remote.api.StaticApi
import com.ledger.personal.data.remote.api.TransactionNetworkDataSource
import com.ledger.personal.data.remote.api.TransactionRetrofitApi
import com.ledger.personal.data.remote.certificatePinner
import com.ledger.personal.data.remote.interceptor.AuthInterceptor
import com.ledger.personal.data.remote.interceptor.MockInterceptor
import com.ledger.personal.data.remote.interceptor.StaticCacheInterceptor
import com.ledger.personal.data.remote.interceptor.StaticFreshnessInterceptor
import com.ledger.personal.domain.pay.BillPay
import com.ledger.personal.domain.release.AppRelease
import com.ledger.personal.domain.release.ReleaseNotice
import com.ledger.personal.domain.session.OtpRepository
import com.ledger.personal.domain.session.OtpService
import com.ledger.personal.domain.session.Session
import com.ledger.personal.domain.settings.CategoryCatalog
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

private const val TIMEOUT_SECONDS = 30L
private const val STATIC_CACHE_BYTES = 2L * 1024 * 1024
private const val JSON_MEDIA_TYPE = "application/json"

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

    /** Hardened authenticated OkHttpClient engine. */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedOkHttpClient(
        config: ApiConfig,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        mockInterceptor: MockInterceptor,
        @ApplicationContext context: Context,
    ): OkHttpClient = finish(
        timeouts().addInterceptor(authInterceptor),
        config,
        context,
        loggingInterceptor,
        mockInterceptor,
    )

    /** Public OkHttpClient engine for static endpoints. No Authorization header; cached on disk. */
    @Provides
    @Singleton
    @StaticClient
    fun provideStaticOkHttpClient(
        config: ApiConfig,
        loggingInterceptor: HttpLoggingInterceptor,
        mockInterceptor: MockInterceptor,
        @ApplicationContext context: Context,
    ): OkHttpClient = finish(
        timeouts()
            .cache(Cache(File(context.cacheDir, "static-http"), STATIC_CACHE_BYTES))
            .addInterceptor(StaticCacheInterceptor())
            .addNetworkInterceptor(StaticFreshnessInterceptor()),
        config,
        context,
        loggingInterceptor,
        mockInterceptor,
    )

    /** Team OkHttpClient engine. */
    @Provides
    @Singleton
    @TeamClient
    fun provideTeamOkHttpClient(
        config: ApiConfig,
        authInterceptor: AuthInterceptor,
        loggingInterceptor: HttpLoggingInterceptor,
        mockInterceptor: MockInterceptor,
        @ApplicationContext context: Context,
    ): OkHttpClient = finish(
        timeouts().addInterceptor(authInterceptor),
        config,
        context,
        loggingInterceptor,
        mockInterceptor,
    )

    /** Authenticated Retrofit instance for API calls. */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideAuthenticatedRetrofit(
        @AuthenticatedClient okHttpClient: OkHttpClient,
        config: ApiConfig,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    /** Static Retrofit instance for public cached API calls. */
    @Provides
    @Singleton
    @StaticClient
    fun provideStaticRetrofit(
        @StaticClient okHttpClient: OkHttpClient,
        config: ApiConfig,
        json: Json,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(config.baseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()

    /** Supplies the authenticated Ktor client for WebSockets. */
    @Provides
    @Singleton
    @AuthenticatedClient
    fun provideHttpClient(
        @AuthenticatedClient engineClient: OkHttpClient,
        config: ApiConfig,
        json: Json,
    ): HttpClient = ktorClient(
        engineClient = engineClient,
        config = config,
        json = json,
        authenticated = true,
    )

    /** Supplies the session. Lock drops the token; unlock posts a new one. */
    @Provides
    @Singleton
    fun provideSession(session: InterceptorSession): Session = session

    /** Supplies Session Retrofit API. */
    @Provides
    @Singleton
    fun provideSessionRetrofitApi(@StaticClient retrofit: Retrofit): SessionRetrofitApi =
        retrofit.create(SessionRetrofitApi::class.java)

    /** Supplies OTP repository for phone verification. */
    @Provides
    @Singleton
    fun provideOtpRepository(repository: RetrofitOtpRepository): OtpRepository = repository

    /** Supplies OTP Retrofit API. */
    @Provides
    @Singleton
    fun provideOtpRetrofitApi(@StaticClient retrofit: Retrofit): OtpRetrofitApi =
        retrofit.create(OtpRetrofitApi::class.java)

    /** Supplies bill pay. Derived AES key from ECDH; key is never posted. */
    @Provides
    @Singleton
    fun provideBillPay(pay: InterceptorBillPay): BillPay = pay

    /** Supplies Bill Pay Retrofit API. */
    @Provides
    @Singleton
    fun provideBillPayRetrofitApi(@AuthenticatedClient retrofit: Retrofit): BillPayRetrofitApi =
        retrofit.create(BillPayRetrofitApi::class.java)

    /** Supplies the transaction Retrofit API. */
    @Provides
    @Singleton
    fun provideTransactionRetrofitApi(@AuthenticatedClient retrofit: Retrofit): TransactionRetrofitApi =
        retrofit.create(TransactionRetrofitApi::class.java)

    /** Supplies the transaction network data source following Android Architecture Samples. */
    @Provides
    @Singleton
    fun provideTransactionNetworkDataSource(
        dataSource: RetrofitTransactionNetworkDataSource,
    ): TransactionNetworkDataSource = dataSource

    /** Supplies the static Retrofit API. */
    @Provides
    @Singleton
    fun provideStaticApi(@StaticClient retrofit: Retrofit): StaticApi =
        retrofit.create(StaticApi::class.java)

    /** Supplies the dashboard Retrofit API. */
    @Provides
    @Singleton
    fun provideDashboardRetrofitApi(@AuthenticatedClient retrofit: Retrofit): DashboardRetrofitApi =
        retrofit.create(DashboardRetrofitApi::class.java)

    /** Supplies the payments Retrofit API. */
    @Provides
    @Singleton
    fun providePaymentsRetrofitApi(
        @TeamClient okHttpClient: OkHttpClient,
        config: ApiConfig,
        json: Json,
    ): PaymentsRetrofitApi = Retrofit.Builder()
        .baseUrl(config.paymentsBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(PaymentsRetrofitApi::class.java)

    /** Supplies the promotions Retrofit API. */
    @Provides
    @Singleton
    fun providePromotionsRetrofitApi(
        @TeamClient okHttpClient: OkHttpClient,
        config: ApiConfig,
        json: Json,
    ): PromotionsRetrofitApi = Retrofit.Builder()
        .baseUrl(config.promotionsBaseUrl)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
        .create(PromotionsRetrofitApi::class.java)

    /** Supplies category catalog. A failed call yields an empty list so a saved catalog is kept. */
    @Provides
    @Singleton
    fun provideCategoryCatalog(api: StaticApi): CategoryCatalog =
        CategoryCatalog {
            runCatching { api.categories().categories }
                .onFailure { if (it is CancellationException) throw it }
                .getOrDefault(emptyList())
        }

    /** Supplies app release. A failed call yields null so a downed static API does not lock the app. */
    @Provides
    @Singleton
    fun provideAppRelease(api: StaticApi): AppRelease = AppRelease {
        coroutineScope {
            val version = async {
                runCatching { api.version() }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrNull()
            }
            val status = async {
                runCatching { api.status() }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrNull()
            }
            ReleaseNotice(version.await()?.versionCode, status.await()?.softUpdate == true, status.await()?.versionCode ?: 0)
        }
    }

    /**
     * Builds a Ktor client on the hardened OkHttp engine for WebSocket ticker connections.
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
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)

    /** Adds logging, mock interception, debug inspection, and certificate pins, then builds the client. */
    private fun finish(
        builder: OkHttpClient.Builder,
        config: ApiConfig,
        context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        mockInterceptor: MockInterceptor,
    ): OkHttpClient {
        if (config.mockFlavor) {
            builder.addInterceptor(mockInterceptor)
        }
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
