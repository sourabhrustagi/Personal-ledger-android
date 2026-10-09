package com.ledger.personal.di

data class ApiConfig(
    val baseUrl: String,
    val logBodies: Boolean,
    val debuggable: Boolean,
    val pinSha256: String,
    val pinBackup: String,
)
