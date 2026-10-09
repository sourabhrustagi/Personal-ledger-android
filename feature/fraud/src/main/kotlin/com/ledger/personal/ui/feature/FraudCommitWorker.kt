package com.ledger.personal.ui.feature

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ledger.personal.data.local.FraudBlockQueue
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class FraudCommitWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(ID) ?: return Result.success()
        FraudBlockQueue.commit(id)
        return Result.success()
    }

    private companion object {
        const val ID = "id"
    }
}

object FraudCommitSchedule {
    fun enqueue(context: Context, id: String) {
        val request = OneTimeWorkRequestBuilder<FraudCommitWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setInputData(workDataOf("id" to id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(name(id), ExistingWorkPolicy.KEEP, request)
    }

    fun cancel(context: Context, id: String) {
        WorkManager.getInstance(context).cancelUniqueWork(name(id))
        FraudBlockQueue.undo(id)
    }

    private fun name(id: String) = "ledger-fraud-commit-$id"
}
