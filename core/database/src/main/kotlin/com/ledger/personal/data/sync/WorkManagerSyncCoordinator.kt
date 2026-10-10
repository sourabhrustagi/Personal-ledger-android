package com.ledger.personal.data.sync

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.SyncPhase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Coordinates work manager sync. */
@Singleton
class WorkManagerSyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
) : SyncCoordinator {
    /** Enqueue. */
    override fun enqueue() {
        WorkManager.getInstance(context).enqueueUniqueWork(
            TransactionSync.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            TransactionSync.request(),
        )
    }

    /** Observe. */
    override fun observe(): Flow<SyncPhase> =
        WorkManager.getInstance(context)
            .getWorkInfosForUniqueWorkFlow(TransactionSync.WORK_NAME)
            .map { infos -> infos.firstOrNull()?.toPhase() ?: SyncPhase.IDLE }
}

/** Converts this value to phase. */
private fun WorkInfo.toPhase(): SyncPhase = when (state) {
    WorkInfo.State.ENQUEUED -> SyncPhase.ENQUEUED
    WorkInfo.State.RUNNING -> SyncPhase.RUNNING
    WorkInfo.State.SUCCEEDED -> SyncPhase.SUCCEEDED
    WorkInfo.State.FAILED -> SyncPhase.FAILED
    WorkInfo.State.BLOCKED -> SyncPhase.BLOCKED
    WorkInfo.State.CANCELLED -> SyncPhase.IDLE
}
