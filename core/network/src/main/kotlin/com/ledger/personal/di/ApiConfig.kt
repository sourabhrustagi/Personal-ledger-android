package com.ledger.personal.di

import javax.inject.Qualifier

/** Api config. */
data class ApiConfig(
    val baseUrl: String,
    val logRequests: Boolean,
    val debuggable: Boolean,
    val pinSha256: String,
    val pinBackup: String,
    val paymentsBaseUrl: String,
    val promotionsBaseUrl: String,
    val mockFlavor: Boolean,
) {
    /** True when running on the mock build flavor. */
    val isMockFlavor: Boolean get() = mockFlavor
}

/** Authenticated client that sends the current session token. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/** Public client for cacheable static endpoints. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StaticClient

/** Authenticated client with no default host, so each team is called at its own base URL. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TeamClient
