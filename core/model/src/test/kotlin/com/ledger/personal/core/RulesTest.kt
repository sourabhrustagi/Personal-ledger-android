package com.ledger.personal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {

    @Test
    fun splitRoundingPreservesTotal() {
        val parts = splitEvenly(100L, 3)
        assertEquals(listOf(34L, 33L, 33L), parts)
        assertEquals(100L, parts.sum())
    }

    @Test
    fun rejectsUnbalancedPosting() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            post(
                listOf(
                    Leg("cash", debitMinor = 100L, creditMinor = 0L),
                    Leg("income", debitMinor = 0L, creditMinor = 90L),
                ),
            )
        }
        assertTrue(exception.message!!.contains("differ"))
    }

    @Test
    fun sameIdempotencyKeyProducesOneTransfer() {
        val book = TransferBook()
        assertEquals(500L, book.submit("key-1", 500L))
        assertEquals(500L, book.submit("key-1", 500L))
        assertEquals(1, book.size())
    }

    @Test
    fun otherUsersWalletIsNotFound() {
        assertEquals(WalletLookup.NotFound, lookupWallet(actor = "A", owner = "B"))
        assertEquals(WalletLookup.Found("A"), lookupWallet(actor = "A", owner = "A"))
    }

    @Test
    fun alertFiresOnceOnThresholdCross() {
        val watch = ThresholdWatch(threshold = 100L)
        assertFalse(watch.onRate(90L))
        assertTrue(watch.onRate(100L))
        assertFalse(watch.onRate(130L))
        assertFalse(watch.onRate(80L))
        assertTrue(watch.onRate(101L))
    }

    @Test
    fun adapterRejectsMissingField() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            adaptQuote(mapOf("pair" to "USDINR"))
        }
        assertTrue(exception.message!!.contains("ticks"))
    }

    @Test
    fun lowConfidenceRoutesToHumanReview() {
        assertTrue(needsHumanReview(listOf(CapturedField(required = true, confidence = 0.5))))
        assertFalse(needsHumanReview(listOf(CapturedField(required = true, confidence = 0.95))))
    }

    @Test
    fun concurrentEditsConflictInsteadOfOverwrite() {
        val merged = mergeField(left = "12 Park St", right = "14 Park St")
        assertTrue(merged is FieldMerge.Conflict)
        assertEquals("12 Park St", (merged as FieldMerge.Conflict).left)
    }

    @Test
    fun replayNeverDoubleCharges() {
        val queue = ChargeQueue()
        queue.replay(listOf("pay-1", "pay-1", "pay-2"))
        queue.replay(listOf("pay-1"))
        assertEquals(2, queue.charges)
    }

    @Test
    fun emiOfFiveLakhAtTwelvePercentOverThirtySixMonths() {
        val principal = 50_000_000L
        assertEquals(1_660_715L, emiMinor(principal, annualBps = 1_200, months = 36))
        val legs = amortize(principal, annualBps = 1_200, months = 36)
        assertEquals(principal, legs.sumOf { it.principalMinor })
    }

    @Test
    fun prepaymentAtSameEmiShortensTenure() {
        val emi = emiMinor(50_000_000L, annualBps = 1_200, months = 36)
        assertEquals(36, tenureFor(50_000_000L, 1_200, emi))
        assertEquals(28, tenureFor(40_000_000L, 1_200, emi))
        assertEquals(0, tenureFor(0L, 1_200, emi))
    }

    @Test
    fun statementJobIsIdempotentPerCycle() {
        val job = StatementJob()
        assertTrue(job.run("2026-10"))
        assertFalse(job.run("2026-10"))
        assertTrue(job.run("2026-11"))
    }

    @Test
    fun logsDoNotKeepAPan() {
        val raw = "charge 4111111111111111 ok"
        val clean = redactPans(raw)
        assertFalse(clean.contains("4111111111111111"))
        assertTrue(luhn("4111111111111111"))
    }

    @Test
    fun categoryQueryUsesTheIndex() {
        val ledger = CategoryLedger()
        repeat(20_000) { index -> ledger.add(if (index % 10 == 0) "Dining" else "Other", 100L) }
        val started = System.nanoTime()
        val dining = ledger.amounts("Dining")
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertEquals(2_000, dining.size)
        assertTrue(elapsedMs < 50)
    }

    @Test
    fun mockMpinUpiAndCooling() {
        assertTrue(mpinAccepted("1234"))
        assertFalse(mpinAccepted("12"))
        assertEquals("upi://pay?pa=asha@ok&am=10.50", upiPayload("asha@ok", 1050L))
        assertTrue(coolingBlocks(addedAtEpochMs = 0L, nowEpochMs = 60_000L))
        assertFalse(coolingBlocks(addedAtEpochMs = 0L, nowEpochMs = 31 * 60 * 1000L))
    }
}
