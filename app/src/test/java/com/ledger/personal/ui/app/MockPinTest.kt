package com.ledger.personal.ui.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPinTest {
    @Test
    fun onlyTheMockFlavorAccepts1111() {
        assertTrue(mockPinUnlocks("1111", mockFlavor = true))
        assertFalse(mockPinUnlocks("1111", mockFlavor = false))
        assertFalse(mockPinUnlocks("2580", mockFlavor = true))
    }
}
