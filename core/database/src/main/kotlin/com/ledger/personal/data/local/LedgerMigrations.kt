package com.ledger.personal.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    /** Migrate. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE transactions ADD COLUMN updatedAtEpochMs INTEGER NOT NULL DEFAULT 0",
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    /** Migrate. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `kyc_drafts` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, " +
                "`confidence` REAL NOT NULL, `captureUri` TEXT, PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `assistant_messages` (`token` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, PRIMARY KEY(`token`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `pos_forward` (`idempotencyKey` TEXT NOT NULL, " +
                "`amountMinor` INTEGER NOT NULL, `settled` INTEGER NOT NULL, PRIMARY KEY(`idempotencyKey`))",
        )
    }
}
