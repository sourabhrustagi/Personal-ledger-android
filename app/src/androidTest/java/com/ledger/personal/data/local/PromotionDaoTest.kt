package com.ledger.personal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PromotionDaoTest {
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
    fun cachesAndObservesPromotionsInRoom() = runBlocking {
        val dao = database.promotionDao()
        val initial = dao.observe().first()
        assertEquals(0, initial.size)

        val promo = PromotionEntity(
            id = "festive-1",
            shelfId = "festive",
            shelfTitle = "Festive Deals",
            pitch = "10% cashback up to ₹200",
            until = "Until Diwali",
            position = 0,
        )
        dao.insertAll(listOf(promo))

        val observed = dao.observe().first()
        assertEquals(1, observed.size)
        assertEquals("festive-1", observed.first().id)
        assertEquals("Festive Deals", observed.first().shelfTitle)

        dao.clear()
        val cleared = dao.observe().first()
        assertEquals(0, cleared.size)
    }
}
