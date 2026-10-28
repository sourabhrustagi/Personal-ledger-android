package com.ledger.personal.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ledger.personal.domain.settings.UserSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Background work for transaction sync. */
@HiltWorker
class TransactionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settings: UserSettings,
) : CoroutineWorker(context, params) {
    /** Do work. */
    override suspend fun doWork(): Result {
        settings.recordSync(System.currentTimeMillis())
        return Result.success()
    }
}

