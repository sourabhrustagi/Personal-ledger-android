package com.ledger.personal.domain.release

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppReleaseTest {
    @Test
    fun anOlderBuildMustUpdate() {
        assertTrue(updateRequired(currentVersionCode = 1, requiredVersionCode = 2))
        assertFalse(updateRequired(currentVersionCode = 2, requiredVersionCode = 2))
        assertFalse(updateRequired(currentVersionCode = 3, requiredVersionCode = 2))
        assertFalse(updateRequired(currentVersionCode = 1, requiredVersionCode = null))
    }
}
