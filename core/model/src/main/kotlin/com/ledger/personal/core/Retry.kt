package com.ledger.personal.core

import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** The server is briefly unable to answer. Sending again with the same idempotency key is safe. */
class ServerBusy(message: String = "The bank's server is busy") : IOException(message)

/** The server answered and refused. Sending again gets the same answer, so it is never retried. */
class TransactionRejected(message: String, cause: Throwable? = null) : Exception(message, cause)

const val TRANSACTION_ATTEMPTS = 3

/**
 * Runs [block] up to [attempts] times. Only [IOException] (dropped connection, timeout, [ServerBusy]) is retried,
 * waiting [firstDelayMs] and then doubling. Any other failure returns at once.
 * [block] must be idempotent: it is called again after a response may have been lost.
 */
suspend inline fun <T> withRetry(
    attempts: Int = TRANSACTION_ATTEMPTS,
    firstDelayMs: Long = 1_000,
    onAttempt: (Int) -> Unit = {},
    block: suspend () -> T,
): Result<T> {
    require(attempts >= 1) { "attempts must be at least 1" }
    var wait = firstDelayMs
    for (attempt in 1..attempts) {
        onAttempt(attempt)
        try {
            return Result.success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            if (attempt == attempts) return Result.failure(error)
            delay(wait)
            wait *= 2
        } catch (error: Exception) {
            return Result.failure(error)
        }
    }
    error("unreachable")
}

/** What the customer reads when a write did not go through. */
fun transactionFailureMessage(error: Throwable): String = when (error) {
    is TransactionRejected -> "${error.message}. Nothing was changed on the server."
    is IOException ->
        "We couldn't confirm this after $TRANSACTION_ATTEMPTS tries on a weak connection. " +
            "If it already went through it will show shortly. The same reference is reused, so it won't run twice."
    else -> "This didn't go through. Nothing was changed on the server."
}
