package com.ledger.personal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ledger.personal.domain.model.Money
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var database: LedgerDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun storesMoneyAsMinorUnits() = runBlocking {
        database.transactionDao().upsert(
            TransactionEntity(
                id = "tx-1",
                amount = Money(1250L),
                currency = "USD",
                category = "Dining",
                description = "Lunch",
                timestampEpochMs = 1_715_000_000_000L,
                syncState = SyncState.SYNCED,
                updatedAtEpochMs = 1_715_000_000_000L,
            ),
        )
        val stored = database.transactionDao().allVisible().single()
        assertEquals(1250L, stored.amount.minorUnits)
        assertEquals("Dining", stored.category)
    }
}
