package com.ledger.personal.domain.diagnostics

import java.util.concurrent.ConcurrentHashMap

/**
 * Structured diagnostic trace modeled after Firebase Performance Monitoring traces.
 * Records wall-clock duration, CPU duration, and custom metric counters.
 */
class PerformanceTrace(
    val name: String,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nanoClock: () -> Long = { System.nanoTime() },
) {
    private var startTimeMs: Long = 0L
    private var stopTimeMs: Long = 0L
    private var startNano: Long = 0L
    private var stopNano: Long = 0L
    private val metrics = ConcurrentHashMap<String, Long>()
    private val attributes = ConcurrentHashMap<String, String>()

    var isRunning: Boolean = false
        private set

    /** Starts the performance trace. */
    fun start(): PerformanceTrace {
        require(!isRunning) { "Trace '$name' is already running" }
        startTimeMs = clock()
        startNano = nanoClock()
        isRunning = true
        return this
    }

    /** Stops the performance trace. */
    fun stop(): Long {
        require(isRunning) { "Trace '$name' is not running" }
        stopTimeMs = clock()
        stopNano = nanoClock()
        isRunning = false
        return durationMs
    }

    /** Total elapsed duration in milliseconds. */
    val durationMs: Long
        get() = if (isRunning) {
            clock() - startTimeMs
        } else {
            stopTimeMs - startTimeMs
        }

    /** Increments a custom named metric counter. */
    fun incrementMetric(metricName: String, value: Long = 1L) {
        metrics.compute(metricName) { _, current -> (current ?: 0L) + value }
    }

    /** Sets a custom attribute for trace classification. */
    fun putAttribute(key: String, value: String) {
        attributes[key] = value
    }

    /** Gets the value of a recorded metric, or 0 if unset. */
    fun getMetric(metricName: String): Long = metrics[metricName] ?: 0L

    /** Gets a snapshot of all attributes. */
    fun getAttributes(): Map<String, String> = attributes.toMap()

    /** Gets a snapshot of all metrics. */
    fun getMetrics(): Map<String, Long> = metrics.toMap()
}

/**
 * Inline execution helper to measure the duration of a block with a [PerformanceTrace].
 */
inline fun <T> traceExecution(traceName: String, block: (PerformanceTrace) -> T): T {
    val trace = PerformanceTrace(traceName).start()
    return try {
        block(trace)
    } finally {
        if (trace.isRunning) {
            trace.stop()
        }
    }
}
