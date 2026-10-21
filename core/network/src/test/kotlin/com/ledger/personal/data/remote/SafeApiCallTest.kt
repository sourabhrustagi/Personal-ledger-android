package com.ledger.personal.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class SafeApiCallTest {

    private val testDispatcher = Dispatchers.Unconfined

    @Test
    fun returnsSuccessWhenApiCallSucceeds(): Unit = runBlocking {
        val result = safeApiCall(testDispatcher) { "response-data" }
        assertTrue(result.isSuccess)
        assertEquals("response-data", result.getOrNull())
    }

    @Test
    fun returnsFailureWhenApiCallThrowsNonCancellationException(): Unit = runBlocking {
        val exception = IOException("Network failure")
        val result = safeApiCall<String>(testDispatcher) { throw exception }
        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test(expected = CancellationException::class)
    fun rethrowsCancellationExceptionToPreserveStructuredConcurrency(): Unit = runBlocking {
        safeApiCall<String>(testDispatcher) {
            throw CancellationException("Cancelled")
        }
    }
}
