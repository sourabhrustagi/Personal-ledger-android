package com.ledger.personal.data.diagnostics

import android.os.Handler
import android.os.Looper
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Diagnostic watchdog that monitors the main looper for thread starvation,
 * long-running IPC/SDK lock contention, and impending ANRs (Application Not Responding).
 */
class AnrWatchdog(
    private val mainHandler: Handler = Handler(Looper.getMainLooper()),
    val timeoutMs: Long = DEFAULT_ANR_TIMEOUT_MS,
    private val onAnrDetected: (AnrReport) -> Unit = { report ->
        Timber.e("ANR Watchdog detected thread starvation: %s", report.summary)
    },
) {
    private val isRunning = AtomicBoolean(false)
    private val lastHeartbeat = AtomicLong(0L)
    private var workerThread: Thread? = null

    /** Starts the watchdog thread. */
    @Synchronized
    fun start() {
        if (isRunning.getAndSet(true)) return

        lastHeartbeat.set(System.currentTimeMillis())
        workerThread = Thread({
            while (isRunning.get()) {
                val pingTime = System.currentTimeMillis()
                lastHeartbeat.set(pingTime)

                // Post heartbeat to main looper
                mainHandler.post {
                    lastHeartbeat.set(System.currentTimeMillis())
                }

                try {
                    Thread.sleep(CHECK_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                }

                val elapsed = System.currentTimeMillis() - lastHeartbeat.get()
                if (elapsed >= timeoutMs && isRunning.get()) {
                    val mainThread = Looper.getMainLooper().thread
                    val allStacks = Thread.getAllStackTraces()
                    val report = AnrReport(
                        durationBlockedMs = elapsed,
                        mainThreadState = mainThread.state,
                        mainThreadStackTrace = mainThread.stackTrace,
                        allThreadStacks = allStacks,
                    )
                    onAnrDetected(report)
                }
            }
        }, "ANR-Watchdog-Worker").apply {
            isDaemon = true
            start()
        }
    }

    /** Stops the watchdog thread. */
    @Synchronized
    fun stop() {
        isRunning.set(false)
        workerThread?.interrupt()
        workerThread = null
    }

    companion object {
        const val DEFAULT_ANR_TIMEOUT_MS = 4000L
        const val CHECK_INTERVAL_MS = 1000L
    }
}

/**
 * Structured diagnostic report captured when main thread contention exceeds ANR thresholds.
 */
data class AnrReport(
    val durationBlockedMs: Long,
    val mainThreadState: Thread.State,
    val mainThreadStackTrace: Array<StackTraceElement>,
    val allThreadStacks: Map<Thread, Array<StackTraceElement>>,
) {
    val summary: String
        get() = "Main thread blocked for ${durationBlockedMs}ms in state $mainThreadState at ${
            mainThreadStackTrace.firstOrNull() ?: "unknown"
        }"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as AnrReport
        return durationBlockedMs == other.durationBlockedMs && mainThreadState == other.mainThreadState
    }

    override fun hashCode(): Int = (durationBlockedMs xor (durationBlockedMs ushr 32)).toInt()
}
