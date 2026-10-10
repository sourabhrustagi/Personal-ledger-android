package com.ledger.personal.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/** Database row for kyc draft. */
@Entity(tableName = "kyc_drafts")
data class KycDraftEntity(
    @PrimaryKey val id: String,
    val name: String,
    val confidence: Double,
    val captureUri: String?,
)

/** Database row for assistant message. */
@Entity(tableName = "assistant_messages")
data class AssistantMessageEntity(
    @PrimaryKey val token: String,
    val position: Int,
)

/** Database row for pos forward. */
@Entity(tableName = "pos_forward")
data class PosForwardEntity(
    @PrimaryKey val idempotencyKey: String,
    val amountMinor: Long,
    val settled: Int,
)

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
