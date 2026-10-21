package com.ledger.personal.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Executes a network call safely on [dispatcher], guaranteeing main-safety,
 * catching non-fatal exceptions into [Result.failure], and propagating [CancellationException]
 * so coroutine cancellation is respected.
 */
suspend inline fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    crossinline apiCall: suspend () -> T,
): Result<T> = withContext(dispatcher) {
    runCatching {
        apiCall()
    }.onFailure { throwable ->
        if (throwable is CancellationException) throw throwable
    }
}
