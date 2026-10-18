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

    @Test
    fun aStatusOfferCanBeSkipped() {
        assertTrue(softUpdateDue(softUpdate = true, versionCode = 2, ignoredVersionCode = 0))
        assertTrue(softUpdateDue(softUpdate = true, versionCode = 3, ignoredVersionCode = 2))
        assertFalse(softUpdateDue(softUpdate = false, versionCode = 2, ignoredVersionCode = 0))
        assertFalse(softUpdateDue(softUpdate = true, versionCode = 2, ignoredVersionCode = 2))
    }
}
