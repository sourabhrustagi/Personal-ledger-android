package com.ledger.personal.mock

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException

/** Debug-only levers for the mock flavor; every screen reads the same switches. */
object MockSwitches {
    val slow = MutableStateFlow(false)
    val failures = MutableStateFlow(false)
    val offline = MutableStateFlow(false)
    val empty = MutableStateFlow(false)
    val dropLive = MutableStateFlow(false)
    val forceUpdate = MutableStateFlow(false)

    const val SLOW_MS = 2_500L

    suspend fun <T> gate(block: () -> Result<T>): Result<T> {
        if (slow.value) delay(SLOW_MS)
        return when {
            offline.value -> Result.failure(IOException("No connection"))
            failures.value -> Result.failure(IOException("Server error (mock)"))
            else -> block()
        }
    }

    fun reset() {
        listOf(slow, failures, offline, empty, dropLive, forceUpdate).forEach { it.value = false }
    }
}
