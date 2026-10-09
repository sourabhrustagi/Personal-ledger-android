package com.ledger.personal.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LedgerMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        LedgerDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migratesPopulatedDatabaseWithoutDroppingRows() {
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO transactions (id, amount, currency, category, description, timestampEpochMs, syncState) " +
                    "VALUES ('tx-1', 4550, 'USD', 'Dining', 'Team lunch', 1715000000000, 'SYNCED')",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        migrated.query("SELECT amount, updatedAtEpochMs, description FROM transactions WHERE id = 'tx-1'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(4550L, cursor.getLong(0))
            assertEquals(0L, cursor.getLong(1))
            assertEquals("Team lunch", cursor.getString(2))
        }
        migrated.close()
    }

    @Test
    fun migratesVersionTwoWithoutDroppingRows() {
        helper.createDatabase(TEST_DB_V2, 2).apply {
            execSQL(
                "INSERT INTO transactions (id, amount, currency, category, description, timestampEpochMs, syncState, updatedAtEpochMs) " +
                    "VALUES ('tx-2', 4550, 'USD', 'Dining', 'Team lunch', 1715000000000, 'SYNCED', 5)",
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB_V2, 3, true, MIGRATION_2_3)
        migrated.query("SELECT amount, description FROM transactions WHERE id = 'tx-2'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
            assertEquals(4550L, cursor.getLong(0))
            assertEquals("Team lunch", cursor.getString(1))
        }
        migrated.query("SELECT name FROM sqlite_master WHERE type = 'table' AND name = 'kyc_drafts'").use { cursor ->
            assertEquals(true, cursor.moveToFirst())
        }
        migrated.close()
    }

    private companion object {
        const val TEST_DB = "migration-test"
        const val TEST_DB_V2 = "migration-test-v2"
    }
}
