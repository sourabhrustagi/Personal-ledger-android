package com.ledger.personal.domain.release

/** Result of GET static/version and GET static/status. Null minimum means the version call failed. */
data class ReleaseNotice(
    val minimumVersionCode: Int? = null,
    val softUpdate: Boolean = false,
    val softVersionCode: Int = 0,
)

/** Published version and status from the static API. */
interface AppRelease {
    suspend fun notice(): ReleaseNotice
}

/** True when this build is older than the minimum versionCode from the static API. */
fun updateRequired(currentVersionCode: Int, requiredVersionCode: Int?): Boolean =
    requiredVersionCode != null && currentVersionCode < requiredVersionCode

/** True when GET static/status offers a build this install has not skipped. */
fun softUpdateDue(softUpdate: Boolean, versionCode: Int, ignoredVersionCode: Int): Boolean =
    softUpdate && versionCode > ignoredVersionCode
