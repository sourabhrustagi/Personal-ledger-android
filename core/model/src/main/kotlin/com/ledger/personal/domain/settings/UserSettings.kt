package com.ledger.personal.domain.settings

import kotlinx.coroutines.flow.Flow

interface UserSettings {
    val lastSyncEpochMs: Flow<Long?>
    suspend fun recordSync(epochMs: Long)
}
