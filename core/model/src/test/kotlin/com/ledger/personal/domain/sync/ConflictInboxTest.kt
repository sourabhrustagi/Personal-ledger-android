package com.ledger.personal.domain.sync

import com.ledger.personal.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConflictInboxTest {
    @Test
    fun roundTripsAndReplacesTheSameTransaction() {
        val file = kotlin.io.path.createTempFile("conflicts").toFile()
        val local = Transaction("1", 100L, "INR", "Dining", "Lunch\nwith note", 5L)
        ConflictInbox(file).add(SyncConflict(local, local.copy(description = "Lunch (web)", amountInCents = 200L)))
        val loaded = ConflictInbox(file).conflicts.value.single()
        assertEquals("Lunch\nwith note", loaded.local.description)
        assertEquals(200L, loaded.server.amountInCents)
        ConflictInbox(file).remove("1")
        assertTrue(ConflictInbox(file).conflicts.value.isEmpty())
    }
}
