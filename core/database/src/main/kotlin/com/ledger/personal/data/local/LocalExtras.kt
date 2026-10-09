package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "kyc_drafts")
data class KycDraftEntity(
    @PrimaryKey val id: String,
    val name: String,
    val confidence: Double,
    val captureUri: String?,
)

@Entity(tableName = "assistant_messages")
data class AssistantMessageEntity(
    @PrimaryKey val token: String,
    val position: Int,
)

@Entity(tableName = "pos_forward")
data class PosForwardEntity(
    @PrimaryKey val idempotencyKey: String,
    val amountMinor: Long,
    val settled: Int,
)

@Dao
interface KycDraftDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: KycDraftEntity)

    @Query("SELECT * FROM kyc_drafts WHERE id = :id")
    suspend fun find(id: String): KycDraftEntity?
}

@Dao
interface AssistantDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: AssistantMessageEntity)

    @Query("SELECT * FROM assistant_messages ORDER BY position")
    suspend fun all(): List<AssistantMessageEntity>
}

@Dao
interface PosForwardDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(entity: PosForwardEntity)

    @Query("SELECT COUNT(*) FROM pos_forward")
    suspend fun count(): Int

    @Query("SELECT * FROM pos_forward WHERE settled = 0")
    suspend fun unsettled(): List<PosForwardEntity>

    @Query("UPDATE pos_forward SET settled = 1 WHERE settled = 0")
    suspend fun settleAll(): Int
}
