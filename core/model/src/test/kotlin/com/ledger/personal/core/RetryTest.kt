package com.ledger.personal.core

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class RetryTest {
    @Test
    fun dropsAndBusyServerAreRetriedUntilSuccess() = runBlocking {
        var calls = 0
        val result = withRetry(firstDelayMs = 0) {
            calls++
            when (calls) {
                1 -> throw IOException("dropped")
                2 -> throw ServerBusy()
                else -> "ok"
            }
        }
        assertEquals("ok", result.getOrNull())
        assertEquals(3, calls)
    }

    @Test
    fun declineIsNotRetried() = runBlocking {
        var calls = 0
        val result = withRetry(firstDelayMs = 0) {
            calls++
            throw TransactionRejected("Your bank declined this payment")
        }
        assertEquals(1, calls)
        assertTrue(transactionFailureMessage(result.exceptionOrNull()!!).startsWith("Your bank declined"))
    }

    @Test
    fun givesUpAfterTheLastAttempt() = runBlocking {
        var calls = 0
        val result = withRetry(firstDelayMs = 0) {
            calls++
            throw IOException("dropped")
        }
        assertEquals(TRANSACTION_ATTEMPTS, calls)
        assertTrue(result.exceptionOrNull() is IOException)
    }
}
