package com.ledger.personal.data.remote

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CertificatePinsTest {
    @Test
    fun blankPinsLeaveTheClientUnpinned() {
        assertNull(certificatePinner("", ""))
        assertNull(certificatePinner("abc", ""))
    }

    @Test
    fun bothPinsBuildAPinner() {
        val pin = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        assertNotNull(certificatePinner(pin, pin))
    }
}
