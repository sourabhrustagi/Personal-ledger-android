package com.ledger.personal.domain.sync

import kotlinx.coroutines.flow.Flow

enum class SyncPhase {
    IDLE,
    ENQUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    BLOCKED,
}

fun SyncPhase.label(): String? = when (this) {
    SyncPhase.IDLE -> null
    SyncPhase.ENQUEUED, SyncPhase.BLOCKED -> "Waiting for network"
    SyncPhase.RUNNING -> "Syncing…"
    SyncPhase.SUCCEEDED -> "Synced"
    SyncPhase.FAILED -> "Sync failed"
}

interface SyncCoordinator {
    fun enqueue()
    fun observe(): Flow<SyncPhase>
}
