package com.ledger.personal.data.sync

import com.ledger.personal.data.local.InspectionDao
import com.ledger.personal.data.local.InspectionFormEntity
import com.ledger.personal.data.local.InspectionPhotoEntity
import com.ledger.personal.domain.servicing.MediaUploadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumableUploadTest {

    private class FakeInspectionDao : InspectionDao {
        val photos = mutableMapOf<String, InspectionPhotoEntity>()

        override fun observeForm(id: String): Flow<InspectionFormEntity?> = flowOf(null)
        override suspend fun findForm(id: String): InspectionFormEntity? = null
        override fun observePendingForms(): Flow<List<InspectionFormEntity>> = flowOf(emptyList())
        override suspend fun upsertForm(form: InspectionFormEntity) = Unit
        override suspend fun deleteForm(id: String) = Unit

        override fun observePhotosForForm(formId: String): Flow<List<InspectionPhotoEntity>> =
            flowOf(photos.values.filter { it.formId == formId })

        override suspend fun getPendingPhotos(): List<InspectionPhotoEntity> =
            photos.values.filter { it.uploadStatus != MediaUploadStatus.COMPLETED }

        override suspend fun findPhoto(photoId: String): InspectionPhotoEntity? = photos[photoId]

        override suspend fun upsertPhoto(photo: InspectionPhotoEntity) {
            photos[photo.photoId] = photo
        }

        override suspend fun updatePhotoProgress(photoId: String, bytesUploaded: Long, status: String) {
            val existing = photos[photoId]
            if (existing != null) {
                photos[photoId] = existing.copy(bytesUploaded = bytesUploaded, uploadStatus = status)
            }
        }

        override suspend fun deletePhoto(photoId: String) {
            photos.remove(photoId)
        }
    }

    @Test
    fun resumesUploadFromSavedByteOffsetAfterMidStreamNetworkDrop() = runTest {
        val dao = FakeInspectionDao()
        val uploadManager = ResumableUploadManager(dao)

        val totalBytes = 1_000_000L // 1 MB file
        val chunkSize = 250_000L // 250 KB chunks

        val initialPhoto = InspectionPhotoEntity(
            photoId = "photo-1",
            formId = "form-101",
            localFilePath = "/data/photos/inspection_living_room.jpg",
            fileSizeBytes = totalBytes,
            bytesUploaded = 0L,
            checksum = "sha256_mock_hash",
            remoteUrl = null,
            uploadStatus = MediaUploadStatus.QUEUED,
        )
        dao.upsertPhoto(initialPhoto)

        // Attempt 1: Network drops after 2 chunks (500 KB)
        var chunkCount = 0
        val dropResult = uploadManager.uploadChunked(
            photo = initialPhoto,
            chunkSizeBytes = chunkSize,
            onSimulatedNetworkDrop = {
                chunkCount++
                chunkCount == 3 // Drop on 3rd chunk
            },
        )

        assertTrue(dropResult.isFailure)
        val partiallyUploaded = dao.findPhoto("photo-1")
        assertEquals(500_000L, partiallyUploaded?.bytesUploaded)
        assertEquals(MediaUploadStatus.FAILED, partiallyUploaded?.uploadStatus)

        // Attempt 2: Reconnection; resume from 500 KB rather than restarting from 0!
        val resumeResult = uploadManager.uploadChunked(
            photo = partiallyUploaded!!,
            chunkSizeBytes = chunkSize,
            onSimulatedNetworkDrop = null, // Stable network connection
        )

        assertTrue(resumeResult.isSuccess)
        val completedPhoto = dao.findPhoto("photo-1")
        assertEquals(totalBytes, completedPhoto?.bytesUploaded)
        assertEquals(MediaUploadStatus.COMPLETED, completedPhoto?.uploadStatus)
        assertTrue(completedPhoto?.remoteUrl?.contains("photo-1.jpg") == true)
    }
}
