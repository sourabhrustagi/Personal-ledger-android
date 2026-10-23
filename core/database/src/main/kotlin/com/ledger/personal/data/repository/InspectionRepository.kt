package com.ledger.personal.data.repository

import com.ledger.personal.data.local.InspectionDao
import com.ledger.personal.data.local.InspectionFormEntity
import com.ledger.personal.data.local.InspectionPhotoEntity
import com.ledger.personal.data.local.toDomain
import com.ledger.personal.data.local.toEntity
import com.ledger.personal.data.sync.ResumableUploadManager
import com.ledger.personal.domain.servicing.FormMergeResult
import com.ledger.personal.domain.servicing.InspectionForm
import com.ledger.personal.domain.servicing.InspectionPhoto
import com.ledger.personal.domain.servicing.MediaUploadStatus
import com.ledger.personal.domain.servicing.mergeInspectionForms
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface representing remote backend communication for inspection synchronization.
 */
interface RemoteInspectionClient {
    suspend fun submitForm(form: InspectionForm, photoUrls: List<String>): Result<InspectionForm>
}

/**
 * Offline-first repository coordinating zero-data-loss local persistence in Room,
 * resumable photo streaming, and 3-way merge conflict handling.
 */
@Singleton
class InspectionRepository @Inject constructor(
    private val dao: InspectionDao,
    private val uploadManager: ResumableUploadManager,
) {
    /** Reactive stream of the local inspection form from Room. */
    fun observeForm(id: String): Flow<InspectionForm?> =
        dao.observeForm(id).map { it?.toDomain() }

    /** Reactive stream of inspection photo outbox items. */
    fun observePhotos(formId: String): Flow<List<InspectionPhoto>> =
        dao.observePhotosForForm(formId).map { list -> list.map(InspectionPhotoEntity::toDomain) }

    /**
     * Saves form draft and photo metadata locally first before attempting sync.
     * Guarantees zero data loss if reception drops or app is terminated.
     */
    suspend fun saveDraft(form: InspectionForm, photos: List<InspectionPhoto>) = withContext(Dispatchers.IO) {
        dao.upsertForm(form.toEntity())
        photos.forEach { dao.upsertPhoto(it.toEntity()) }
    }

    /**
     * Executes offline sync: uploads all pending photos with byte-level resumption,
     * then pushes the form payload with optimistic locking.
     */
    suspend fun syncInspection(
        formId: String,
        remoteClient: RemoteInspectionClient,
    ): Result<InspectionForm> = withContext(Dispatchers.IO) {
        val form = dao.findForm(formId)?.toDomain()
            ?: return@withContext Result.failure(NoSuchElementException("Inspection form not found: $formId"))

        // 1. Upload photos chunk-by-chunk with resumable tracking
        val uploadedUrls = mutableListOf<String>()
        val pendingPhotos = dao.getPendingPhotos().filter { it.formId == formId }
        for (photo in pendingPhotos) {
            val uploadResult = uploadManager.uploadChunked(photo)
            if (uploadResult.isFailure) {
                return@withContext Result.failure(uploadResult.exceptionOrNull() ?: Exception("Photo upload failed"))
            }
            uploadedUrls.add(uploadResult.getOrThrow())
        }

        // 2. Submit form payload to remote switch
        val pushResult = remoteClient.submitForm(form, uploadedUrls)
        if (pushResult.isSuccess) {
            val confirmed = pushResult.getOrThrow().copy(syncStatus = "SYNCED")
            dao.upsertForm(confirmed.toEntity())
            return@withContext Result.success(confirmed)
        }

        return@withContext pushResult
    }

    /**
     * Handles 3-way merge when a 409 conflict is received from the server.
     */
    suspend fun handleConflict(
        base: InspectionForm,
        server: InspectionForm,
        formId: String,
    ): FormMergeResult = withContext(Dispatchers.IO) {
        val local = dao.findForm(formId)?.toDomain()
            ?: throw NoSuchElementException("Local form missing for conflict: $formId")

        val result = mergeInspectionForms(base, local, server)
        dao.upsertForm(result.merged.toEntity())
        result
    }
}
