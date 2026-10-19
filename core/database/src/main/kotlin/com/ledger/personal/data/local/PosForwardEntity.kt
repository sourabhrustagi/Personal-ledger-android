package com.ledger.personal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Database row for pos forward. */
@Entity(tableName = "pos_forward")
data class PosForwardEntity(
    @PrimaryKey val idempotencyKey: String,
    val amountMinor: Long,
    val settled: Int,
)
