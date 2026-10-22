package com.ledger.personal.data.remote

/**
 * Standardized HTTP header definitions for production-grade banking and financial applications.
 *
 * Enforces banking regulatory compliance, distributed tracing, idempotency, device fingerprinting,
 * strong customer authentication (SCA), and fraud detection standards.
 */
object BankingHeaders {

    // --- Distributed Tracing & Observability ---
    /** Unique identifier across end-to-end distributed microservice calls (APM/tracing). */
    const val X_CORRELATION_ID = "X-Correlation-Id"

    /** Unique identifier for a single HTTP network request attempt. */
    const val X_REQUEST_ID = "X-Request-Id"

    /** W3C or vendor trace context identifier. */
    const val X_TRACE_ID = "X-Trace-Id"

    /** Milliseconds or ISO-8601 timestamp when the request was initiated on the device. */
    const val X_TIMESTAMP = "X-Timestamp"

    // --- Financial Integrity & Anti-Fraud ---
    /** Idempotency key ensuring mutating financial transactions (transfers, debits) execute exactly once. */
    const val X_IDEMPOTENCY_KEY = "X-Idempotency-Key"

    /** Alternative RFC-compliant idempotency key header. */
    const val IDEMPOTENCY_KEY = "Idempotency-Key"

    /** Banking channel identifier (e.g. mobile_banking, retail_pos, internet_banking). */
    const val X_CHANNEL = "X-Channel"

    // --- Security, Session & Device Binding ---
    /** Standard HTTP authentication header containing Bearer token. */
    const val AUTHORIZATION = "Authorization"

    /** Authenticated user banking session identifier. */
    const val X_SESSION_ID = "X-Session-Id"

    /** Unique cryptographic hardware or KeyStore device identifier bound to the user account. */
    const val X_DEVICE_ID = "X-Device-Id"

    /** Standard device identifier header. */
    const val DEVICE_ID = "Device-Id"

    /** Hash or fingerprint of hardware parameters for anomaly and fraud scoring. */
    const val X_DEVICE_FINGERPRINT = "X-Device-Fingerprint"

    /** Digital payload signature (e.g., ECDSA SHA-256) proving non-repudiation and request integrity. */
    const val X_SIGNATURE = "X-Signature"

    /** Public key identifier or raw key corresponding to the device signature. */
    const val X_PUBLIC_KEY = "X-Public-Key"

    // --- Client Environment & Telemetry ---
    /** Operating system platform (e.g. android, ios). */
    const val X_PLATFORM = "X-Platform"

    /** Standard platform header. */
    const val PLATFORM = "Platform"

    /** Client app semantic version (e.g. 1.0.0). */
    const val X_APP_VERSION = "X-App-Version"

    /** Client build number / version code. */
    const val X_APP_BUILD = "X-App-Build"

    /** Mobile OS version or API level (e.g. 34 for Android 14). */
    const val X_OS_VERSION = "X-OS-Version"

    /** Hardware device model (e.g. Pixel 8). */
    const val X_DEVICE_MODEL = "X-Device-Model"

    /** Device hardware manufacturer (e.g. Google, Samsung). */
    const val X_DEVICE_MANUFACTURER = "X-Device-Manufacturer"

    /** User's preferred device locale tag (e.g. en-IN, hi-IN). */
    const val X_DEVICE_LOCALE = "X-Device-Locale"

    // --- Standard Content & Cache Headers ---
    /** Accepted response payload representation. */
    const val ACCEPT = "Accept"

    /** Content type of the outgoing request body. */
    const val CONTENT_TYPE = "Content-Type"

    /** Natural language preferences of the user. */
    const val ACCEPT_LANGUAGE = "Accept-Language"

    /** User-Agent header detailing client identity and system properties. */
    const val USER_AGENT = "User-Agent"

    /** Directives for caching mechanisms in requests and responses. */
    const val CACHE_CONTROL = "Cache-Control"

    // --- Common Banking Header Values ---
    /** Standard JSON MIME content type. */
    const val APPLICATION_JSON = "application/json"

    /** Bearer authorization prefix. */
    const val BEARER_PREFIX = "Bearer "

    /** Android platform value. */
    const val PLATFORM_ANDROID = "android"

    /** Channel value for retail mobile banking. */
    const val CHANNEL_MOBILE_BANKING = "mobile_banking"

    /** Cache-Control directive to prevent storing sensitive banking responses on disk. */
    const val NO_STORE = "no-store, no-cache, must-revalidate"
}
