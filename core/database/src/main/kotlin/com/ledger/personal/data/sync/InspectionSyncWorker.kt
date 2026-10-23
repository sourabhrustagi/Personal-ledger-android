package com.ledger.personal.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.ledger.personal.data.local.InspectionDao
import com.ledger.personal.domain.servicing.MediaUploadStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.IOException

/**
 * Background WorkManager worker orchestrating resilient upload of large inspection photos
 * and form payloads with network constraints and exponential backoff retry.
 */
@HiltWorker
class InspectionSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dao: InspectionDao,
    private val uploadManager: ResumableUploadManager,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val formId = inputData.getString(KEY_FORM_ID) ?: return Result.failure()

        val pendingPhotos = dao.getPendingPhotos().filter { it.formId == formId }
        for (photo in pendingPhotos) {
            val result = uploadManager.uploadChunked(photo)
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                return if (error is IOException) {
                    Result.retry() // Connection dropped; WorkManager retries with backoff
                } else {
                    Result.failure()
                }
            }
        }

        // Form payload marks ready for sync once all photos are confirmed
        val form = dao.findForm(formId)
        if (form != null && form.syncStatus == "PENDING_CREATE") {
            dao.upsertForm(form.copy(syncStatus = "SYNCED"))
        }

        return Result.success()
    }

    companion object {
        const val KEY_FORM_ID: String = "form_id"

        /** Builds work request with network connectivity constraints. */
        fun buildRequest(formId: String): OneTimeWorkRequest =
            OneTimeWorkRequestBuilder<InspectionSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build(),
                )
                .setInputData(workDataOf(KEY_FORM_ID to formId))
                .build()
    }
}
