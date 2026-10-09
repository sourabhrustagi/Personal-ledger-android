package com.ledger.personal.data.tools

import com.ledger.personal.data.local.MemoryPosForward
import com.ledger.personal.data.security.LocalBindingServer
import com.ledger.personal.data.security.SIGNATURE_ALGORITHM
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.ui.feature.splitShares
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class ToolsTest {
    private fun tx(id: String, minor: Long, category: String = "Dining", day: LocalDate = LocalDate.of(2026, 10, 5)) = Transaction(
        id = id,
        amountInCents = minor,
        currency = "INR",
        category = category,
        description = "Lunch $id",
        timestampEpochMs = day.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
    )

    @Test
    fun parsesUpiQrAndRejectsOtherCodes() {
        val target = parseUpi("upi://pay?pa=chai.point@okicici&pn=Chai%20Point&am=120.00")
        assertEquals(UpiTarget("chai.point@okicici", "Chai Point", 12_000L), target)
        assertEquals(0L, parseUpi("upi://pay?pa=a@b")?.amountMinor)
        assertNull(parseUpi("https://example.com"))
        assertNull(parseUpi("upi://pay?pn=NoAddress"))
    }

    @Test
    fun conflictListsOnlyDifferingFieldsAndMergesChoices() {
        val local = tx("1", 45_000L)
        val server = local.copy(description = "Lunch (web)", amountInCents = 46_000L)
        assertEquals(listOf("Description", "Amount (minor units)"), conflictFields(local, server).map { it.field })
        val merged = resolveConflict(local, server, mapOf("Description" to true, "Amount (minor units)" to false))
        assertEquals("Lunch 1", merged.description)
        assertEquals(46_000L, merged.amountInCents)
    }

    @Test
    fun statementSplitsMoneyInAndOutForTheMonth() {
        val rows = listOf(tx("a", 10_000L), tx("b", 85_000_00L, "Salary"), tx("c", 5_000L, day = LocalDate.of(2026, 9, 30)))
        val summary = statementFor(rows, YearMonth.of(2026, 10), ZoneOffset.UTC)
        assertEquals(2, summary.rows.size)
        assertEquals(85_000_00L, summary.inMinor)
        assertEquals(10_000L, summary.outMinor)
    }

    @Test
    fun filingTwiceForOneTransactionKeepsOneDispute() {
        val desk = DisputeDesk(null)
        val first = desk.file(tx("1", 100L), DisputeReason.DUPLICATE, "Charged twice at the store", now = 1_000L)
        val again = desk.file(tx("1", 100L), DisputeReason.WRONG_AMOUNT, "Different text", now = 2_000L)
        assertEquals(first, again)
        assertEquals(1, desk.disputes.value.size)
        assertEquals(0, first.stage(now = 1_000L))
        assertEquals(1, first.stage(now = 1_000L + REVIEW_AFTER_MS))
    }

    @Test
    fun aFiledDisputeIsStillThereAfterRestart() {
        val file = kotlin.io.path.createTempFile("disputes").toFile()
        DisputeDesk(file).file(tx("1", 100L), DisputeReason.DUPLICATE, "Charged twice at the store", now = 1_000L)
        val reloaded = DisputeDesk(file)
        assertEquals("DSP-001000", reloaded.disputes.value.single().reference)
    }

    @Test
    fun bindingServerAcceptsOnlyValidOneTimeSignatures() = runTest {
        val server = LocalBindingServer()
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        val challenge = server.challenge()
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(pair.private)
            update(challenge)
            sign()
        }
        assertTrue(server.register(pair.public.encoded, challenge, signature).isSuccess)
        assertTrue(server.register(pair.public.encoded, challenge, signature).isFailure)
        val fresh = server.challenge()
        assertTrue(server.register(pair.public.encoded, fresh, signature).isFailure)
    }

    @Test
    fun posRefundNetsAgainstSaleAtSettlement() = runTest {
        val store = MemoryPosForward()
        store.enqueue("pos-sale-1", 50_000L)
        store.enqueue("refund-pos-sale-1", -50_000L)
        store.enqueue("refund-pos-sale-1", -50_000L)
        store.enqueue("pos-sale-2", 12_000L)
        assertEquals(12_000L, store.unsettled().sum())
        assertEquals(3, store.settleAll())
        assertEquals(0, store.unsettled().size)
        assertEquals(0, store.settleAll())
    }

    @Test
    fun splitGivesExactSharesWithYouFirst() {
        val shares = splitShares(100_00L, listOf("You", "asha@okaxis", "rahul@oksbi"))
        assertEquals(listOf(33_34L, 33_33L, 33_33L), shares.map { it.second })
        assertEquals("You", shares.first().first)
    }
}
