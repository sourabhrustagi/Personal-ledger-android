package com.ledger.personal.domain.release

/** Minimum versionCode published by GET static/version. */
interface AppRelease {
    /** Null when the static API does not answer. */
    suspend fun minimumVersionCode(): Int?
}

/** True when this build is older than the versionCode from the static API. */
fun updateRequired(currentVersionCode: Int, requiredVersionCode: Int?): Boolean =
    requiredVersionCode != null && currentVersionCode < requiredVersionCode
