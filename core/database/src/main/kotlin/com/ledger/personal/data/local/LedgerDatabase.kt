package com.ledger.personal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        PromotionEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
/** Ledger database containing only cached promotions. */
abstract class LedgerDatabase : RoomDatabase() {
    /** Promotion dao. */
    abstract fun promotionDao(): PromotionDao

    companion object {
        const val NAME = "ledger.db"
    }
}

