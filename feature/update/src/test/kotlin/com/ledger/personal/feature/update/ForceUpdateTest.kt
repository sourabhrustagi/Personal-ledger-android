package com.ledger.personal.feature.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ForceUpdateTest {
    @Test
    fun debugBuildsOpenTheStoreApp() {
        assertEquals("com.ledger.personal", PlayStore.storeListingId("com.ledger.personal.debug"))
        assertEquals("com.ledger.personal.mock", PlayStore.storeListingId("com.ledger.personal.mock.debug"))
        assertEquals("com.ledger.personal", PlayStore.storeListingId("com.ledger.personal"))
    }
}
