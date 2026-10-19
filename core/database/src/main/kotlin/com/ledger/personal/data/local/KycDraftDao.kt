package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Room queries for kyc draft. */
@Dao
interface KycDraftDao {
    /** Upsert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KycDraftEntity)

    /** Find. */
    @Query("SELECT * FROM kyc_drafts WHERE id = :id")
    suspend fun find(id: String): KycDraftEntity?
}
