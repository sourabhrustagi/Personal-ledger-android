package com.ledger.personal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ledger.personal.domain.servicing.InspectionPhoto

/** Database row for offline inspection photo upload tracking. */
@Entity(tableName = "inspection_photos")
data class InspectionPhotoEntity(
    @PrimaryKey val photoId: String,
    val formId: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val bytesUploaded: Long,
    val checksum: String,
    val remoteUrl: String?,
    val uploadStatus: String,
)

fun InspectionPhotoEntity.toDomain(): InspectionPhoto = InspectionPhoto(
    photoId = photoId,
    formId = formId,
    localFilePath = localFilePath,
    fileSizeBytes = fileSizeBytes,
    bytesUploaded = bytesUploaded,
    checksum = checksum,
    remoteUrl = remoteUrl,
    uploadStatus = uploadStatus,
)

fun InspectionPhoto.toEntity(): InspectionPhotoEntity = InspectionPhotoEntity(
    photoId = photoId,
    formId = formId,
    localFilePath = localFilePath,
    fileSizeBytes = fileSizeBytes,
    bytesUploaded = bytesUploaded,
    checksum = checksum,
    remoteUrl = remoteUrl,
    uploadStatus = uploadStatus,
)
