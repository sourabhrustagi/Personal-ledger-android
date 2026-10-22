package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

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

/** Public reference-data contract using Retrofit. */
interface StaticApi {
    /** Returns the default category names. */
    @GET(ApiEndpoints.STATIC_CATEGORIES)
    suspend fun categories(): CategoryCatalogDto

    /** Returns the versionCode this install must reach. */
    @GET(ApiEndpoints.STATIC_VERSION)
    suspend fun version(): VersionDto

    /** Returns whether a newer build can be skipped. */
    @GET(ApiEndpoints.STATIC_STATUS)
    suspend fun status(): StatusDto
}

