package com.ledger.personal.feature.history.switcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryArchitectureTest {

    @Test
    fun verifiesAllEightArchitecturesConfigured() {
        val all = HistoryArchitecture.ALL
        assertEquals(8, all.size)

        val uniqueIds = all.map { it.id }.toSet()
        assertEquals(8, uniqueIds.size)

        all.forEach { arch ->
            assertTrue(arch.displayName.isNotBlank())
            assertTrue(arch.authorOrCompany.isNotBlank())
            assertTrue(arch.paradigm.isNotBlank())
            assertTrue(arch.summary.isNotBlank())
            assertTrue(arch.tags.isNotEmpty())
        }
    }
}
