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

val MIGRATION_4_5 = object : Migration(4, 5) {
    /** Migrate to add offline inspection forms and photos outbox tables. */
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `inspection_forms` (" +
                "`id` TEXT NOT NULL, " +
                "`loanId` TEXT NOT NULL, " +
                "`propertyAddress` TEXT NOT NULL, " +
                "`inspectorNotes` TEXT NOT NULL, " +
                "`damageReport` TEXT NOT NULL, " +
                "`clientUpdatedAtEpochMs` INTEGER NOT NULL, " +
                "`baseServerVersion` INTEGER NOT NULL, " +
                "`syncStatus` TEXT NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `inspection_photos` (" +
                "`photoId` TEXT NOT NULL, " +
                "`formId` TEXT NOT NULL, " +
                "`localFilePath` TEXT NOT NULL, " +
                "`fileSizeBytes` INTEGER NOT NULL, " +
                "`bytesUploaded` INTEGER NOT NULL, " +
                "`checksum` TEXT NOT NULL, " +
                "`remoteUrl` TEXT, " +
                "`uploadStatus` TEXT NOT NULL, " +
                "PRIMARY KEY(`photoId`))",
        )
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

/** Inserts initial dummy scan and pay transactions into the database. */
fun SupportSQLiteDatabase.seedTransactions() {
    val now = 1_760_200_000_000L
    val seeds = listOf(
        Triple("sp-1", 120_00L, "Chai Point · QR Pay"),
        Triple("sp-2", 385_00L, "Starbucks · Scan & Pay"),
        Triple("sp-3", 450_00L, "Swiggy Instamart · QR Pay"),
        Triple("sp-4", 1_450_50L, "DMart Supermarket · Scan & Pay"),
        Triple("sp-5", 312_00L, "Apollo Pharmacy · QR Pay"),
        Triple("sp-6", 60_00L, "Metro Station · Scan & Pay"),
        Triple("sp-7", 280_00L, "Blue Tokai Coffee · QR Pay"),
        Triple("sp-8", 520_00L, "Corner Bookstore · Scan & Pay"),
    )
    seeds.forEachIndexed { i, (id, amount, desc) ->
        val timestamp = now - ((i + 1) * 3_600_000L * 4)
        execSQL(
            "INSERT OR IGNORE INTO transactions (id, amount, currency, category, description, timestampEpochMs, syncState, updatedAtEpochMs) VALUES " +
                "('tx-$id', $amount, 'INR', 'Scan & Pay', '$desc', $timestamp, 'SYNCED', $timestamp)",
        )
    }
}

