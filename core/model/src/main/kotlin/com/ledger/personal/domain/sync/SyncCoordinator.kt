package com.ledger.personal.domain.sync

import kotlinx.coroutines.flow.Flow

/** Allowed values of sync phase. */
enum class SyncPhase(val label: String?) {
    IDLE(null),
    ENQUEUED("Waiting for network"),
    RUNNING("Syncing…"),
    SUCCEEDED("Synced"),
    FAILED("Sync failed"),
    BLOCKED("Waiting for network"),
}

/** Coordinates sync. */
interface SyncCoordinator {
    /** Enqueue. */
    fun enqueue()
    /** Observe. */
    fun observe(): Flow<SyncPhase>
}
