package com.ledger.personal.data.remote.api

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

/** Public reference-data contract. */
interface StaticApi {
    /** Returns the default category names. */
    suspend fun categories(): CategoryCatalogDto

    /** Returns the versionCode this install must reach. */
    suspend fun version(): VersionDto
}

/** Ktor implementation of the public static API. */
@Singleton
class KtorStaticApi @Inject constructor(
    @param:StaticClient private val client: HttpClient,
) : StaticApi {
    /** Returns the default category names. */
    override suspend fun categories(): CategoryCatalogDto = client.get("static/categories").body()

    /** Returns the versionCode this install must reach. */
    override suspend fun version(): VersionDto = client.get("static/version").body()
}
