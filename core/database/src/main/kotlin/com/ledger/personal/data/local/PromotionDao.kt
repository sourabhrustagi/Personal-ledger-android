package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Room queries for promotions. */
@Dao
interface PromotionDao {
    /** Shelves in display order. */
    @Query("SELECT * FROM promotions ORDER BY position")
    fun observe(): Flow<List<PromotionEntity>>
}
