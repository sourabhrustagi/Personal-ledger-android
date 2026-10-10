package com.ledger.personal.domain.sync

import kotlinx.coroutines.flow.Flow

/** Allowed values of sync phase. */
enum class SyncPhase {
    IDLE,
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    BLOCKED,
}

/** Label. */
fun SyncPhase.label(): String? = when (this) {
    SyncPhase.IDLE -> null
    SyncPhase.ENQUEUED, SyncPhase.BLOCKED -> "Waiting for network"
    SyncPhase.RUNNING -> "Syncing…"
    SyncPhase.SUCCEEDED -> "Synced"
    SyncPhase.FAILED -> "Sync failed"
}

/** Coordinates sync. */
interface SyncCoordinator {
    /** Enqueue. */
    fun enqueue()
    /** Observe. */
    fun observe(): Flow<SyncPhase>
}
