package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Data access object for offline inspection forms and large media photo outbox. */
@Dao
interface InspectionDao {

    @Query("SELECT * FROM inspection_forms WHERE id = :id")
    fun observeForm(id: String): Flow<InspectionFormEntity?>

    @Query("SELECT * FROM inspection_forms WHERE id = :id")
    suspend fun findForm(id: String): InspectionFormEntity?

    @Query("SELECT * FROM inspection_forms WHERE syncStatus != 'SYNCED'")
    fun observePendingForms(): Flow<List<InspectionFormEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertForm(form: InspectionFormEntity)

    @Query("DELETE FROM inspection_forms WHERE id = :id")
    suspend fun deleteForm(id: String)

    @Query("SELECT * FROM inspection_photos WHERE formId = :formId ORDER BY photoId ASC")
    fun observePhotosForForm(formId: String): Flow<List<InspectionPhotoEntity>>

    @Query("SELECT * FROM inspection_photos WHERE uploadStatus != 'COMPLETED'")
    suspend fun getPendingPhotos(): List<InspectionPhotoEntity>

    @Query("SELECT * FROM inspection_photos WHERE photoId = :photoId")
    suspend fun findPhoto(photoId: String): InspectionPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPhoto(photo: InspectionPhotoEntity)

    @Query("UPDATE inspection_photos SET bytesUploaded = :bytesUploaded, uploadStatus = :status WHERE photoId = :photoId")
    suspend fun updatePhotoProgress(photoId: String, bytesUploaded: Long, status: String)

    @Query("DELETE FROM inspection_photos WHERE photoId = :photoId")
    suspend fun deletePhoto(photoId: String)
}
