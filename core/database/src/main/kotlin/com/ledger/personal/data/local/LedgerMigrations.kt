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

val MIGRATION_3_4 = object : Migration(3, 4) {
    /** Migrate. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `promotions` (`id` TEXT NOT NULL, `shelfId` TEXT NOT NULL, " +
                "`shelfTitle` TEXT NOT NULL, `pitch` TEXT NOT NULL, `until` TEXT NOT NULL, " +
                "`position` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        db.seedPromotions()
    }
}

/** Inserts the promotion rows. Fresh databases and the 3 to 4 upgrade both use this. */
fun SupportSQLiteDatabase.seedPromotions() {
    execSQL(
        "INSERT OR IGNORE INTO promotions (id, shelfId, shelfTitle, pitch, `until`, position) VALUES " +
            "('upi5', 'festive', 'Festive', '5% back on UPI up to ₹100', 'Until 31 Oct', 0), " +
            "('grocery', 'festive', 'Festive', '₹50 off groceries above ₹500', 'Until 20 Oct', 1), " +
            "('electricity', 'bills', 'Bills', '₹25 cashback on electricity', 'This week', 2)",
    )
}
