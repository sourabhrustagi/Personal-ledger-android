package com.ledger.personal.ui.feature

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun parsesRupeesIntoPaiseWithoutRounding() {
        assertEquals(1050L, parseMinor("10.5"))
        assertEquals(100L, parseMinor("1"))
        assertNull(parseMinor("0"))
        assertNull(parseMinor("1.234"))
        assertNull(parseMinor(""))
    }

    @Test
    fun formatsMinorUnitsInTheAccountCurrency() {
        assertTrue(formatMoney(1_660_715L).contains("16,607.15"))
        assertEquals("$120.00 USD", formatMoney(12_000L, "USD"))
        assertTrue(formatMoney(12_990_050L).contains("1,29,900.50"))
    }

    @Test
    fun signsAndSpeaksAmountsWithoutRelyingOnColour() {
        assertTrue(formatSigned(45_000L, MoneyDirection.DEBIT).startsWith("\u2212₹"))
        assertTrue(formatSigned(1_200_000L, MoneyDirection.CREDIT).startsWith("+₹"))
        assertEquals("450 rupees", spokenAmount(45_000L))
        assertEquals("10 rupees 50 paise", spokenAmount(1_050L))
    }
}
