package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Room queries for promotions. */
@Dao
interface PromotionDao {
    /** Shelves in display order. */
    @Query("SELECT * FROM promotions ORDER BY position")
    fun observe(): Flow<List<PromotionEntity>>

    /** Upsert promotions into Room cache. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(promotions: List<PromotionEntity>)

    /** Clear cached promotions. */
    @Query("DELETE FROM promotions")
    suspend fun clear()
}

