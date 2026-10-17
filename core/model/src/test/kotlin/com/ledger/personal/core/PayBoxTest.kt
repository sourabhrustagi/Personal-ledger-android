package com.ledger.personal.core

import com.ledger.personal.domain.pay.SealedPay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayBoxTest {
    @Test
    fun bothSidesOpenTheSamePlaintext() {
        val server = PayBox.keyPair()
        val phone = PayBox.keyPair()
        val secret = PayBox.agree(phone.private, server.public.encoded)
        val peer = PayBox.agree(server.private, phone.public.encoded)
        assertTrue(secret.contentEquals(peer))
        val packed = PayBox.seal(secret, PayBox.pack("debit-4821", "electricity", 50_000L, "1234"))
        val parts = PayBox.open(peer, packed.first, packed.second).decodeToString().split('|')
        assertEquals(listOf("debit-4821", "electricity", "50000", "1234"), parts)
    }

    @Test
    fun lookupDecryptsAKnownBillAndRejectsAMiss() = runBlocking {
        val rail = LocalBillPay()
        val phone = PayBox.keyPair()
        val secret = PayBox.agree(phone.private, rail.serverPublic())
        val hit = PayBox.seal(secret, PayBox.packLookup("electricity", "220184"))
        val bill = rail.fetch(SealedPay(phone.public.encoded, hit.first, hit.second)).getOrThrow()
        assertEquals("220184", bill.billNumber)
        assertEquals(125_000L, bill.amountMinor)
        val miss = PayBox.seal(secret, PayBox.packLookup("electricity", "999999"))
        assertTrue(rail.fetch(SealedPay(phone.public.encoded, miss.first, miss.second)).isFailure)
    }

    @Test
    fun localRailRejectsAShortPin() = runBlocking {
        val rail = LocalBillPay()
        val phone = PayBox.keyPair()
        val secret = PayBox.agree(phone.private, rail.serverPublic())
        val packed = PayBox.seal(secret, PayBox.pack("debit-4821", "electricity", 50_000L, "12"))
        assertTrue(rail.submit(SealedPay(phone.public.encoded, packed.first, packed.second)).isFailure)
    }

    @Test
    fun electricityPayRequiresTheBillOnFile() = runBlocking {
        val rail = LocalBillPay()
        assertTrue(submit(rail, PayBox.pack("debit-4821", "electricity", 50_000L, "1234", "104829")).isSuccess)
        assertTrue(submit(rail, PayBox.pack("debit-4821", "electricity", 1L, "1234", "104829")).isFailure)
        assertTrue(submit(rail, PayBox.pack("debit-4821", "electricity", 50_000L, "1234")).isFailure)
    }

    @Test
    fun otherBillerPaysWithoutABillNumber() = runBlocking {
        val rail = LocalBillPay()
        assertTrue(submit(rail, PayBox.pack("debit-4821", "water", 32_000L, "1234")).isSuccess)
    }

    @Test
    fun tamperedCiphertextDoesNotOpen() {
        val server = PayBox.keyPair()
        val phone = PayBox.keyPair()
        val secret = PayBox.agree(phone.private, server.public.encoded)
        val packed = PayBox.seal(secret, PayBox.packLookup("electricity", "104829"))
        val flipped = packed.second.copyOf().also { it[0] = (it[0].toInt() xor 1).toByte() }
        assertTrue(runCatching { PayBox.open(secret, packed.first, flipped) }.isFailure)
    }

    private suspend fun submit(rail: LocalBillPay, plain: ByteArray) = rail.submit(seal(rail, plain))

    private suspend fun seal(rail: LocalBillPay, plain: ByteArray): SealedPay {
        val phone = PayBox.keyPair()
        val secret = PayBox.agree(phone.private, rail.serverPublic())
        val packed = PayBox.seal(secret, plain)
        return SealedPay(phone.public.encoded, packed.first, packed.second)
    }
}
