package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.di.StaticClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

/** Network payload for the static category catalog. */
@Serializable
data class CategoryCatalogDto(
    @SerialName("categories") val categories: List<String> = emptyList(),
)

/** Network payload for the minimum supported build. */
@Serializable
data class VersionDto(
    @SerialName("versionCode") val versionCode: Int = 0,
)

/** Network payload for a skippable update. [versionCode] is the build being offered. */
@Serializable
data class StatusDto(
    @SerialName("softUpdate") val softUpdate: Boolean = false,
    @SerialName("versionCode") val versionCode: Int = 0,
)

/** Public reference-data contract. */
interface StaticApi {
    /** Returns the default category names. */
    suspend fun categories(): CategoryCatalogDto

    /** Returns the versionCode this install must reach. */
    suspend fun version(): VersionDto

    /** Returns whether a newer build can be skipped. */
    suspend fun status(): StatusDto
}

/** Ktor implementation of the public static API. */
@Singleton
class KtorStaticApi @Inject constructor(
    @param:StaticClient private val client: HttpClient,
) : StaticApi {
    /** Returns the default category names. */
    override suspend fun categories(): CategoryCatalogDto = client.get(ApiEndpoints.STATIC_CATEGORIES).body()

    /** Returns the versionCode this install must reach. */
    override suspend fun version(): VersionDto = client.get(ApiEndpoints.STATIC_VERSION).body()

    /** Returns whether a newer build can be skipped. */
    override suspend fun status(): StatusDto = client.get(ApiEndpoints.STATIC_STATUS).body()
}
