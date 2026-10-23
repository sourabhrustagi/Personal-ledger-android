package com.ledger.personal.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIntegrityTest {

    @Test
    fun parseTracerPidExtractsCorrectValue() {
        val procStatus = """
            Name:   com.ledger.personal
            State:  S (sleeping)
            Tgid:   1234
            Pid:    1234
            PPid:   567
            TracerPid:  9876
            Uid:    10123   10123   10123   10123
        """.trimIndent()

        assertEquals(9876, IntegrityEvaluator.parseTracerPid(procStatus))
    }

    @Test
    fun parseTracerPidReturnsZeroWhenNoTracer() {
        val procStatus = """
            Name:   com.ledger.personal
            TracerPid:  0
        """.trimIndent()

        assertEquals(0, IntegrityEvaluator.parseTracerPid(procStatus))
    }

    @Test
    fun detectHookingLibrariesFlagsFridaAndXposed() {
        val cleanMaps = """
            7f90000000-7f90010000 r-xp 00000000 00:00 0 /system/lib64/libc.so
            7f90020000-7f90030000 r--p 00000000 00:00 0 /system/framework/framework.jar
        """.trimIndent()
        assertFalse(IntegrityEvaluator.detectHookingLibraries(cleanMaps))

        val fridaMaps = """
            7f90000000-7f90010000 r-xp 00000000 00:00 0 /data/local/tmp/frida-agent-64.so
        """.trimIndent()
        assertTrue(IntegrityEvaluator.detectHookingLibraries(fridaMaps))
    }

    @Test
    fun isTestKeysBuildIdentifiesTestKeysTag() {
        assertTrue(IntegrityEvaluator.isTestKeysBuild("test-keys"))
        assertTrue(IntegrityEvaluator.isTestKeysBuild("release-keys,test-keys"))
        assertFalse(IntegrityEvaluator.isTestKeysBuild("release-keys"))
        assertFalse(IntegrityEvaluator.isTestKeysBuild(null))
    }

    @Test
    fun evaluateYieldsExpectedVerdict() {
        val cleanReport = IntegrityEvaluator.evaluate(emptyList())
        assertEquals(IntegrityVerdict.SECURE, cleanReport.verdict)
        assertFalse(cleanReport.isRooted)

        val proxyReport = IntegrityEvaluator.evaluate(listOf(SecurityThreat.ACTIVE_PROXY_DETECTED))
        assertEquals(IntegrityVerdict.SUSPICIOUS, proxyReport.verdict)
        assertTrue(proxyReport.isProxyActive)

        val rootedReport = IntegrityEvaluator.evaluate(listOf(SecurityThreat.ROOT_BINARY_PRESENT))
        assertEquals(IntegrityVerdict.COMPROMISED, rootedReport.verdict)
        assertTrue(rootedReport.isRooted)
    }
}
