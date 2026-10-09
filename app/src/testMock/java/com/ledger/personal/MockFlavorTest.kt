package com.ledger.personal

import org.junit.Assert.assertTrue
import org.junit.Test

class MockFlavorTest {

    @Test
    fun mockFlavorUsesFakeRepository() {
        assertTrue(BuildConfig.USE_FAKE_REPOSITORY)
    }
}
