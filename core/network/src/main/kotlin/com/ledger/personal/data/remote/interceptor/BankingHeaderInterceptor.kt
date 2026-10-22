package com.ledger.personal.data.remote.interceptor

import android.content.Context
import android.os.Build
import com.ledger.personal.data.remote.BankingHeaders
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Production-ready OkHttp interceptor that attaches essential banking headers to all outgoing requests.
 *
 * Automatically populates:
 * - Content negotiation: [BankingHeaders.ACCEPT] = application/json
 * - Distributed tracing: [BankingHeaders.X_CORRELATION_ID] and [BankingHeaders.X_REQUEST_ID]
 * - Request timestamp: [BankingHeaders.X_TIMESTAMP]
 * - Platform & OS metadata: [BankingHeaders.X_PLATFORM], [BankingHeaders.X_OS_VERSION], [BankingHeaders.X_DEVICE_MODEL]
 * - Banking channel: [BankingHeaders.X_CHANNEL]
 * - Idempotency: [BankingHeaders.X_IDEMPOTENCY_KEY] for mutating calls (POST, PUT, PATCH, DELETE) if not already set
 * - App version & User-Agent telemetry
 */
@Singleton
class BankingHeaderInterceptor @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : Interceptor {

    private var explicitVersion: String? = null
    private var explicitDeviceId: String? = null

    /** Secondary constructor for testing environments without an Android Context. */
    constructor(appVersion: String = "1.0.0", deviceId: String = "test-device-id") : this(android.content.ContextWrapper(null)) {
        this.explicitVersion = appVersion
        this.explicitDeviceId = deviceId
    }

    private val deviceId: String by lazy {
        explicitDeviceId ?: runCatching {
            android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID,
            )?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: runCatching {
            val prefs = context.getSharedPreferences("banking_device_prefs", Context.MODE_PRIVATE)
            val existing = prefs.getString("device_id", null)
            if (!existing.isNullOrBlank()) {
                existing
            } else {
                val generated = UUID.randomUUID().toString()
                prefs.edit().putString("device_id", generated).apply()
                generated
            }
        }.getOrDefault("unknown-device")
    }

    private val appVersion: String by lazy {
        explicitVersion ?: runCatching {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            packageInfo.versionName ?: "1.0.0"
        }.getOrDefault("1.0.0")
    }

    private val userAgent: String by lazy {
        val release = Build.VERSION.RELEASE?.takeIf { it.isNotBlank() } ?: "unknown"
        val model = Build.MODEL?.takeIf { it.isNotBlank() } ?: "Generic"
        "PersonalLedger-Android/$appVersion (Linux; Android $release; $model)"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // Content negotiation
        if (original.header(BankingHeaders.ACCEPT) == null) {
            requestBuilder.header(BankingHeaders.ACCEPT, BankingHeaders.APPLICATION_JSON)
        }
        if (original.body != null && original.header(BankingHeaders.CONTENT_TYPE) == null) {
            requestBuilder.header(BankingHeaders.CONTENT_TYPE, BankingHeaders.APPLICATION_JSON)
        }

        // Distributed tracing: generate correlation ID if caller didn't provide one
        if (original.header(BankingHeaders.X_CORRELATION_ID) == null) {
            requestBuilder.header(BankingHeaders.X_CORRELATION_ID, UUID.randomUUID().toString())
        }

        // Unique request attempt ID
        requestBuilder.header(BankingHeaders.X_REQUEST_ID, UUID.randomUUID().toString())

        // Request timestamp in milliseconds
        requestBuilder.header(BankingHeaders.X_TIMESTAMP, System.currentTimeMillis().toString())

        // Essential: Device Identification (X-Device-Id and Device-Id)
        val resolvedDeviceId = original.header(BankingHeaders.X_DEVICE_ID)
            ?: original.header(BankingHeaders.DEVICE_ID)
            ?: deviceId
        if (resolvedDeviceId.isNotBlank()) {
            if (original.header(BankingHeaders.X_DEVICE_ID) == null) {
                requestBuilder.header(BankingHeaders.X_DEVICE_ID, resolvedDeviceId)
            }
            if (original.header(BankingHeaders.DEVICE_ID) == null) {
                requestBuilder.header(BankingHeaders.DEVICE_ID, resolvedDeviceId)
            }
        }

        // Essential: Platform Identification (X-Platform and Platform)
        if (original.header(BankingHeaders.X_PLATFORM) == null) {
            requestBuilder.header(BankingHeaders.X_PLATFORM, BankingHeaders.PLATFORM_ANDROID)
        }
        if (original.header(BankingHeaders.PLATFORM) == null) {
            requestBuilder.header(BankingHeaders.PLATFORM, BankingHeaders.PLATFORM_ANDROID)
        }

        // Device & OS Telemetry
        requestBuilder.header(BankingHeaders.X_OS_VERSION, Build.VERSION.SDK_INT.toString())
        Build.MODEL?.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header(BankingHeaders.X_DEVICE_MODEL, it)
        }
        Build.MANUFACTURER?.takeIf { it.isNotBlank() }?.let {
            requestBuilder.header(BankingHeaders.X_DEVICE_MANUFACTURER, it)
        }
        val localeTag = Locale.getDefault().toLanguageTag()
        requestBuilder.header(BankingHeaders.X_DEVICE_LOCALE, localeTag)
        if (original.header(BankingHeaders.ACCEPT_LANGUAGE) == null) {
            requestBuilder.header(BankingHeaders.ACCEPT_LANGUAGE, localeTag)
        }
        requestBuilder.header(BankingHeaders.X_CHANNEL, BankingHeaders.CHANNEL_MOBILE_BANKING)
        requestBuilder.header(BankingHeaders.X_APP_VERSION, appVersion)

        // Idempotency key for mutating financial requests
        val method = original.method
        val isMutating = method == "POST" || method == "PUT" || method == "PATCH" || method == "DELETE"
        val hasIdempotencyKey = original.header(BankingHeaders.X_IDEMPOTENCY_KEY) != null ||
            original.header(BankingHeaders.IDEMPOTENCY_KEY) != null
        if (isMutating && !hasIdempotencyKey) {
            requestBuilder.header(BankingHeaders.X_IDEMPOTENCY_KEY, UUID.randomUUID().toString())
        }

        // User-Agent
        if (original.header(BankingHeaders.USER_AGENT) == null) {
            requestBuilder.header(BankingHeaders.USER_AGENT, userAgent)
        }

        return chain.proceed(requestBuilder.build())
    }
}
