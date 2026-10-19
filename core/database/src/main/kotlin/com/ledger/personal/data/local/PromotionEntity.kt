package com.ledger.personal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One promotion saved on the device. Room is the only store for these rows. */
@Entity(tableName = "promotions")
data class PromotionEntity(
    @PrimaryKey val id: String,
    val shelfId: String,
    val shelfTitle: String,
    val pitch: String,
    val until: String,
    val position: Int,
)
