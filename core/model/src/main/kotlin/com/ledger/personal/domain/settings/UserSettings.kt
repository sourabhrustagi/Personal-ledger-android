package com.ledger.personal.domain.settings

import kotlinx.coroutines.flow.Flow

/** Contract for user settings. */
interface UserSettings {
    val lastSyncEpochMs: Flow<Long?>
    /** Records sync. */
    suspend fun recordSync(epochMs: Long)
}
