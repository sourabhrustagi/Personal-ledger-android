package com.ledger.personal.data.security

import com.ledger.personal.domain.security.IntegrityEvaluator
import com.ledger.personal.domain.security.IntegrityVerdict
import com.ledger.personal.domain.security.SecurityThreat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSecurityGuardTest {

    @Test
    fun defaultSuPathsContainCriticalBinaries() {
        assertTrue(IntegrityEvaluator.DEFAULT_SU_PATHS.contains("/system/bin/su"))
        assertTrue(IntegrityEvaluator.DEFAULT_SU_PATHS.contains("/system/xbin/su"))
        assertTrue(IntegrityEvaluator.DEFAULT_SU_PATHS.contains("/sbin/su"))
    }

    @Test
    fun rootDirectoriesIncludeMagiskLocations() {
        assertTrue(IntegrityEvaluator.ROOT_DIRECTORIES.contains("/sbin/.magisk"))
        assertTrue(IntegrityEvaluator.ROOT_DIRECTORIES.contains("/data/adb/magisk"))
    }

    @Test
    fun hookingLibrariesListIncludesFridaAndXposed() {
        assertTrue(IntegrityEvaluator.HOOKING_LIBRARIES.contains("frida-agent"))
        assertTrue(IntegrityEvaluator.HOOKING_LIBRARIES.contains("xposed"))
        assertTrue(IntegrityEvaluator.HOOKING_LIBRARIES.contains("substrate"))
    }

    @Test
    fun evaluationPrioritizesCompromisedOverSuspicious() {
        val report = IntegrityEvaluator.evaluate(
            listOf(
                SecurityThreat.ACTIVE_PROXY_DETECTED,
                SecurityThreat.ROOT_BINARY_PRESENT,
            ),
        )
        assertEquals(IntegrityVerdict.COMPROMISED, report.verdict)
        assertTrue(report.isRooted)
        assertTrue(report.isProxyActive)
    }
}
