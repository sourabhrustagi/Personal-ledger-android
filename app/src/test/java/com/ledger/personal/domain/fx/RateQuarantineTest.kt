package com.ledger.personal.domain.fx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RateQuarantineTest {

    @Test
    fun tenSigmaJumpIsQuarantinedNotServedOrDropped() {
        val history = listOf(1000L, 1002L, 998L, 1001L, 999L, 1000L, 1003L, 997L, 1001L, 999L)
        val jump = RateQuote(pair = "USDINR", ticks = 1100L)

        val disposition = disposeQuote(history, jump)

        assertTrue(disposition is RateDisposition.Quarantined)
        assertEquals(jump, (disposition as RateDisposition.Quarantined).quote)
    }
}
