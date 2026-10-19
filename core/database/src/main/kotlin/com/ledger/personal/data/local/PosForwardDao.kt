package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** Room queries for pos forward. */
@Dao
interface PosForwardDao {
    /** Enqueue. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entity: PosForwardEntity)

    /** Count. */
    @Query("SELECT COUNT(*) FROM pos_forward")
    suspend fun count(): Int

    /** Unsettled. */
    @Query("SELECT * FROM pos_forward WHERE settled = 0")
    suspend fun unsettled(): List<PosForwardEntity>

    /** Settles all. */
    @Query("UPDATE pos_forward SET settled = 1 WHERE settled = 0")
    suspend fun settleAll(): Int
}
