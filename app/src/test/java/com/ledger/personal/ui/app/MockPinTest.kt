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

class OfflineNowTest {
    @Test
    fun mockSwitchOrNoConnection() {
        assertTrue(offlineNow(mockOffline = true, connected = true))
        assertTrue(offlineNow(mockOffline = false, connected = false))
        assertFalse(offlineNow(mockOffline = false, connected = true))
    }

    @Test
    fun hasInternetReturnsFalseWhenManagerIsNull() {
        assertFalse(hasInternet(null))
    }
}
