package com.ledger.personal.domain.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceTraceTest {

    @Test
    fun traceMeasuresDurationAccurately() {
        var currentTime = 1000L
        val trace = PerformanceTrace(
            name = "db_sync_trace",
            clock = { currentTime },
        )

        assertFalse(trace.isRunning)
        trace.start()
        assertTrue(trace.isRunning)

        currentTime += 250L
        assertEquals(250L, trace.durationMs)

        currentTime += 150L
        val finalDuration = trace.stop()
        assertFalse(trace.isRunning)

        assertEquals(400L, finalDuration)
        assertEquals(400L, trace.durationMs)
    }

    @Test
    fun metricsAndAttributesAccumulateCorrectly() {
        val trace = PerformanceTrace("api_latency_trace")
        trace.start()

        trace.incrementMetric("items_processed", 10)
        trace.incrementMetric("items_processed", 5)
        trace.incrementMetric("cache_hits", 2)
        trace.putAttribute("network_type", "WIFI")

        assertEquals(15L, trace.getMetric("items_processed"))
        assertEquals(2L, trace.getMetric("cache_hits"))
        assertEquals("WIFI", trace.getAttributes()["network_type"])

        trace.stop()
    }

    @Test
    fun traceExecutionHelperMeasuresAndCompletes() {
        var completed = false
        val result = traceExecution("helper_trace") { trace ->
            trace.incrementMetric("steps")
            completed = true
            "SUCCESS"
        }

        assertTrue(completed)
        assertEquals("SUCCESS", result)
    }
}
