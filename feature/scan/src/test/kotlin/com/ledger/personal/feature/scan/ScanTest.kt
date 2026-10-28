package com.ledger.personal.feature.scan

import com.ledger.personal.feature.scan.model.UpiTarget
import com.ledger.personal.feature.scan.model.parseUpi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScanTest {
    @Test
    fun parsesUpiQrAndRejectsOtherCodes() {
        val target = parseUpi("upi://pay?pa=chai.point@okicici&pn=Chai%20Point&am=120.00")
        assertEquals(UpiTarget("chai.point@okicici", "Chai Point", 12_000L), target)
        assertEquals(0L, parseUpi("upi://pay?pa=a@b")?.amountMinor)
        assertNull(parseUpi("https://example.com"))
        assertNull(parseUpi("upi://pay?pn=NoAddress"))
    }
}
