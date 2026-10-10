package com.ledger.personal.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Room queries for transaction. */
@Dao
interface TransactionDao {
    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC",
    )
    /** Streams all. */
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC",
    )
    /** Paging source. */
    fun pagingSource(): PagingSource<Int, TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE id = :id AND syncState != 'PENDING_DELETE'",
    )
    /** Streams by id. */
    fun observeById(id: String): Flow<TransactionEntity?>

    /** Find. */
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun find(id: String): TransactionEntity?

    /** Upsert. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC",
    )
    /** All visible. */
    suspend fun allVisible(): List<TransactionEntity>

    /** Pending. */
    @Query("SELECT * FROM transactions WHERE syncState != 'SYNCED'")
    suspend fun pending(): List<TransactionEntity>

    /** Delete. */
    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC LIMIT :limit OFFSET :offset",
    )
    /** Page. */
    suspend fun page(limit: Int, offset: Int): List<TransactionEntity>
}
