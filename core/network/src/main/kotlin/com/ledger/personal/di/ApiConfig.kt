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
    val assistantBaseUrl: String,
    val promotionsBaseUrl: String,
    val localSession: Boolean,
)

/** Ktor client that sends the current session token. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/** Ktor client for public, cacheable static endpoints. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StaticClient

/** Authenticated client with no default host, so each team is called at its own base URL. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TeamClient
