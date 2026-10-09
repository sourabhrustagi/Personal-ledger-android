package com.ledger.personal.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC",
    )
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC",
    )
    fun pagingSource(): PagingSource<Int, TransactionEntity>

    @Query(
        "SELECT * FROM transactions WHERE id = :id AND syncState != 'PENDING_DELETE'",
    )
    fun observeById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun find(id: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TransactionEntity)

    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC",
    )
    suspend fun allVisible(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE syncState != 'SYNCED'")
    suspend fun pending(): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun delete(id: String)

    @Query(
        "SELECT * FROM transactions WHERE syncState != 'PENDING_DELETE' " +
            "ORDER BY timestampEpochMs DESC, id DESC LIMIT :limit OFFSET :offset",
    )
    suspend fun page(limit: Int, offset: Int): List<TransactionEntity>
}
