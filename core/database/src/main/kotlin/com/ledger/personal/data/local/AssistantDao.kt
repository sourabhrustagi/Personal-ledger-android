package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Room queries for assistant. */
@Dao
interface AssistantDao {
    /** Insert. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AssistantMessageEntity)

    /** All. */
    @Query("SELECT * FROM assistant_messages ORDER BY position")
    suspend fun all(): List<AssistantMessageEntity>
}
