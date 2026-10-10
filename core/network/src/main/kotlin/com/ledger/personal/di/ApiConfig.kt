package com.ledger.personal.di

import javax.inject.Qualifier

/** Api config. */
data class ApiConfig(
    val baseUrl: String,
    val logRequests: Boolean,
    val debuggable: Boolean,
    val pinSha256: String,
    val pinBackup: String,
)

/** Ktor client that sends the current session token. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthenticatedClient

/** Ktor client for public, cacheable static endpoints. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StaticClient
