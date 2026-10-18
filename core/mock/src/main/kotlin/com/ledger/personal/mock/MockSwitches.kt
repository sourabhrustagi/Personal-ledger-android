package com.ledger.personal.mock

import com.ledger.personal.core.ServerBusy
import com.ledger.personal.core.TransactionRejected
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import kotlin.random.Random

/** Debug levers for the mock flavor. The app reads them only through the mock binding. */
object MockSwitches {
    val slow = MutableStateFlow(false)
    val failures = MutableStateFlow(false)
    val offline = MutableStateFlow(false)
    val empty = MutableStateFlow(false)
    val dropLive = MutableStateFlow(false)
    val flakyPayments = MutableStateFlow(true)

    const val SLOW_MS = 2_500L
    const val PAYMENT_MS = 600L

    /** Next rolls for [transaction] and [payment], 0–99. Tests drain this; the app uses [Random]. */
    val nextRolls = ArrayDeque<Int>()

    suspend inline fun <T> gate(block: () -> Result<T>): Result<T> {
        if (slow.value) delay(SLOW_MS)
        return when {
            offline.value -> Result.failure(IOException("No connection"))
            failures.value -> Result.failure(IOException("Server error (mock)"))
            else -> block()
        }
    }

    /**
     * One write attempt. With [flakyPayments] on, some attempts never reach the server
     * (dropped connection) and some reach it and fail (busy or rejected).
     */
    suspend fun <T> transaction(random: Random = Random.Default, write: () -> T): T {
        gate { Result.success(Unit) }.getOrThrow()
        if (!flakyPayments.value) return write()
        delay(PAYMENT_MS)
        return when (roll(random)) {
            in 0 until 15 -> throw IOException("Connection dropped. The request was cancelled.")
            in 15 until 28 -> throw ServerBusy("The server could not complete this transaction")
            in 28 until 34 -> throw TransactionRejected("The server rejected this transaction")
            else -> write()
        }
    }

    /**
     * Mock payment rail. Same flakes as [transaction], plus a lost reply after the bank has posted.
     */
    suspend fun <T> payment(random: Random = Random.Default, post: () -> T): T {
        gate { Result.success(Unit) }.getOrThrow()
        if (!flakyPayments.value) return post()
        delay(PAYMENT_MS)
        return when (roll(random)) {
            in 0 until 12 -> throw IOException("Connection dropped before the bank replied")
            in 12 until 20 -> {
                post()
                throw IOException("Connection dropped after the bank posted it")
            }
            in 20 until 30 -> throw ServerBusy()
            in 30 until 34 -> throw TransactionRejected("Your bank declined this payment")
            else -> post()
        }
    }

    /** Next scripted roll, or a fresh one. */
    private fun roll(random: Random): Int = nextRolls.removeFirstOrNull() ?: random.nextInt(100)

    /** Reset. */
    fun reset() {
        listOf(slow, failures, offline, empty, dropLive).forEach { it.value = false }
        flakyPayments.value = true
        nextRolls.clear()
    }
}
