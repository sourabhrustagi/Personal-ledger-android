package com.ledger.personal.data.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnrWatchdogTest {

    @Test
    fun anrReportSummaryFormatsAccurately() {
        val stackTrace = arrayOf(
            StackTraceElement("android.app.QueuedWork", "waitToFinish", "QueuedWork.java", 188),
            StackTraceElement("android.app.ActivityThread", "handleStopActivity", "ActivityThread.java", 4200),
        )
        val report = AnrReport(
            durationBlockedMs = 5200L,
            mainThreadState = Thread.State.BLOCKED,
            mainThreadStackTrace = stackTrace,
            allThreadStacks = emptyMap(),
        )

        assertNotNull(report.summary)
        assertTrue(report.summary.contains("5200ms"))
        assertTrue(report.summary.contains("BLOCKED"))
        assertTrue(report.summary.contains("QueuedWork.waitToFinish"))
    }

    @Test
    fun anrReportEqualityBasedOnDurationAndState() {
        val stackTrace = arrayOf(
            StackTraceElement("com.example.Foo", "bar", "Foo.kt", 10),
        )
        val report1 = AnrReport(
            durationBlockedMs = 4000L,
            mainThreadState = Thread.State.WAITING,
            mainThreadStackTrace = stackTrace,
            allThreadStacks = emptyMap(),
        )
        val report2 = AnrReport(
            durationBlockedMs = 4000L,
            mainThreadState = Thread.State.WAITING,
            mainThreadStackTrace = emptyArray(),
            allThreadStacks = emptyMap(),
        )

        assertEquals(report1, report2)
        assertEquals(report1.hashCode(), report2.hashCode())
    }
}
