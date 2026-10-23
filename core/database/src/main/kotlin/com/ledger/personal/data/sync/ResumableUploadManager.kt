package com.ledger.personal.data.sync

import com.ledger.personal.data.local.InspectionDao
import com.ledger.personal.data.local.InspectionPhotoEntity
import com.ledger.personal.domain.servicing.MediaUploadStatus
import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages resumable chunked photo uploads for large property inspection images.
 * Tracks byte-level upload progress in Room so mid-stream connection drops resume
 * from the exact saved byte offset rather than restarting from 0.
 */
@Singleton
class ResumableUploadManager @Inject constructor(
    private val inspectionDao: InspectionDao,
) {
    /**
     * Uploads the given [photo] in chunks.
     * Starts from [InspectionPhotoEntity.bytesUploaded].
     * [onSimulatedNetworkDrop] can be injected in tests or simulated low-reception modes.
     */
    suspend fun uploadChunked(
        photo: InspectionPhotoEntity,
        chunkSizeBytes: Long = DEFAULT_CHUNK_SIZE_BYTES,
        onSimulatedNetworkDrop: (() -> Boolean)? = null,
    ): Result<String> {
        var currentOffset = photo.bytesUploaded
        val totalBytes = photo.fileSizeBytes

        if (photo.uploadStatus == MediaUploadStatus.COMPLETED && photo.remoteUrl != null) {
            return Result.success(photo.remoteUrl)
        }

        inspectionDao.updatePhotoProgress(
            photoId = photo.photoId,
            bytesUploaded = currentOffset,
            status = MediaUploadStatus.UPLOADING,
        )

        while (currentOffset < totalBytes) {
            if (onSimulatedNetworkDrop?.invoke() == true) {
                inspectionDao.updatePhotoProgress(
                    photoId = photo.photoId,
                    bytesUploaded = currentOffset,
                    status = MediaUploadStatus.FAILED,
                )
                return Result.failure(IOException("Network dropped at byte offset $currentOffset"))
            }

            // Simulate network transit time for chunk
            delay(10)
            currentOffset = minOf(currentOffset + chunkSizeBytes, totalBytes)

            // Persist progress to Room immediately after each chunk
            inspectionDao.updatePhotoProgress(
                photoId = photo.photoId,
                bytesUploaded = currentOffset,
                status = if (currentOffset >= totalBytes) MediaUploadStatus.COMPLETED else MediaUploadStatus.UPLOADING,
            )
        }

        val remoteUrl = "https://storage.mortgageservice.internal/inspections/${photo.formId}/${photo.photoId}.jpg"
        inspectionDao.upsertPhoto(photo.copy(
            bytesUploaded = totalBytes,
            uploadStatus = MediaUploadStatus.COMPLETED,
            remoteUrl = remoteUrl,
        ))

        return Result.success(remoteUrl)
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE_BYTES: Long = 256 * 1024L // 256 KB
    }
}
