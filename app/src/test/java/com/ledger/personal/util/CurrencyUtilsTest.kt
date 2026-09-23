package com.ledger.personal.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun convertsMinorUnitsToFormattedCurrency() {
        assertEquals("$12.50", 1250L.toFormattedCurrency("USD"))
        assertEquals("$0.05", 5L.toFormattedCurrency("USD"))
        assertEquals("$1,000.00", 100000L.toFormattedCurrency("USD"))
    }

    @Test
    fun convertsMinorUnitsToMajorUnitsString() {
        assertEquals("12.50", 1250L.toMajorUnitsString())
        assertEquals("0.05", 5L.toMajorUnitsString())
        assertEquals("12.00", 1200L.toMajorUnitsString())
        assertEquals("0.00", 0L.toMajorUnitsString())
    }

    @Test
    fun parsesValidDecimalStringsToMinorUnits() {
        assertEquals(1250L, "12.50".toMinorUnits())
        assertEquals(1250L, "12.5".toMinorUnits())
        assertEquals(1200L, "12".toMinorUnits())
        assertEquals(5L, "0.05".toMinorUnits())
        assertEquals(1200L, "12.".toMinorUnits())
        assertEquals(1250L, "12,50".toMinorUnits())
        assertEquals(1250L, " 12.50 ".toMinorUnits())
        assertEquals(1299L, "12.999".toMinorUnits())
    }

    @Test
    fun rejectsInvalidOrZeroInput() {
        assertNull("0".toMinorUnits())
        assertNull("0.00".toMinorUnits())
        assertNull("-5.00".toMinorUnits())
        assertNull("abc".toMinorUnits())
        assertNull("".toMinorUnits())
        assertNull("   ".toMinorUnits())
        assertNull("1.2.3".toMinorUnits())
    }

    @Test
    fun formatsEpochAsHumanReadableDate() {
        val formatted = 1_715_000_000_000L.toFormattedDateTime()
        assertTrue(formatted.isNotBlank())
        assertEquals(2, formatted.split(",").size)
    }
}
