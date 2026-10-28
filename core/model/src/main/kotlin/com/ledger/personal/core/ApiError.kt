package com.ledger.personal.core

import java.io.IOException
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Standardized API error hierarchy conforming to Google Play financial app production patterns.
 * Provides user-friendly titles, clean messages, and indicates whether the operation can be retried.
 */
sealed class ApiError(
    val title: String,
    val userMessage: String,
    val isRetriable: Boolean,
    cause: Throwable? = null,
) : Exception(userMessage, cause) {

    /** Device is offline or DNS lookup failed. */
    class Network(
        title: String = "No connection",
        message: String = "No internet connection. Please check your network and try again.",
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = true, cause = cause)

    /** Request or connection timed out. */
    class Timeout(
        title: String = "Connection timed out",
        message: String = "The server took too long to respond. Please try again.",
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = true, cause = cause)

    /** 5xx Server Error or server busy. */
    class Server(
        title: String = "Service unavailable",
        message: String = "Our servers are currently unavailable. Please try again in a few moments.",
        val statusCode: Int? = null,
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = true, cause = cause)

    /** 401/403 Unauthorized or session expired. */
    class Unauthorized(
        title: String = "Session expired",
        message: String = "Your session has expired. Please sign in again.",
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = false, cause = cause)

    /** 409 Conflict. */
    class Conflict(
        title: String = "Version conflict",
        message: String = "This entry was modified elsewhere. Please refresh to view the latest data.",
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = true, cause = cause)

    /** 429 Rate limit exceeded. */
    class RateLimit(
        title: String = "Too many attempts",
        message: String = "Too many attempts. Please wait a moment before trying again.",
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = false, cause = cause)

    /** 4xx Client validation or business rule rejection. */
    class Client(
        title: String = "Invalid request",
        message: String = "Please check your details and try again.",
        val statusCode: Int? = null,
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = false, cause = cause)

    /** Fallback unexpected error. */
    class Unknown(
        title: String = "Something went wrong",
        message: String = "An unexpected error occurred. Please try again.",
        cause: Throwable? = null,
    ) : ApiError(title, message, isRetriable = true, cause = cause)
}

/** Formats any [Throwable] into a Google Play production quality, user-friendly error message. */
fun formatApiError(error: Throwable): String = toApiError(error).userMessage

/** Formats a string error representation into a clean, human-readable user message. */
fun formatApiError(rawMessage: String): String {
    val trimmed = rawMessage.trim()
    if (trimmed.isEmpty()) return "An unexpected error occurred. Please try again."
    return when {
        trimmed.contains("HTTP 5", ignoreCase = true) || trimmed.contains("500", ignoreCase = true) ->
            "Our servers are currently unavailable. Please try again shortly."
        trimmed.contains("HTTP 401", ignoreCase = true) || trimmed.contains("HTTP 403", ignoreCase = true) ->
            "Your session has expired. Please sign in again."
        trimmed.contains("HTTP 409", ignoreCase = true) ->
            "This entry was modified elsewhere. Please refresh to view the latest data."
        trimmed.contains("HTTP 429", ignoreCase = true) ->
            "Too many attempts. Please wait a moment before trying again."
        trimmed.contains("Unable to resolve host", ignoreCase = true) ||
            trimmed.contains("No address associated", ignoreCase = true) ||
            trimmed.contains("Failed to connect", ignoreCase = true) ->
            "No internet connection. Please check your network and try again."
        trimmed.contains("timeout", ignoreCase = true) ->
            "The connection timed out. Please try again."
        isHumanFriendly(trimmed) -> trimmed
        else -> "Something went wrong. Please try again."
    }
}

/** Converts any [Throwable] into a categorized [ApiError]. */
fun toApiError(error: Throwable): ApiError = when (error) {
    is ApiError -> error
    is UnknownHostException, is ConnectException -> ApiError.Network(cause = error)
    is SocketTimeoutException, is InterruptedIOException -> ApiError.Timeout(cause = error)
    is ServerBusy -> ApiError.Server(cause = error)
    is TransactionRejected -> ApiError.Client(
        title = "Transaction declined",
        message = error.message?.takeIf(::isHumanFriendly) ?: "Your transaction was declined.",
        cause = error,
    )
    is IOException -> {
        val msg = error.message.orEmpty()
        when {
            msg.contains("Unable to resolve host", ignoreCase = true) ||
                msg.contains("Failed to connect", ignoreCase = true) -> ApiError.Network(cause = error)
            msg.contains("timeout", ignoreCase = true) -> ApiError.Timeout(cause = error)
            isHumanFriendly(msg) -> ApiError.Client(message = msg, cause = error)
            else -> ApiError.Network(cause = error)
        }
    }
    is IllegalArgumentException, is IllegalStateException -> {
        val msg = error.message.orEmpty()
        ApiError.Client(
            message = if (isHumanFriendly(msg)) msg else "Invalid request. Please check your details.",
            cause = error,
        )
    }
    else -> {
        val msg = error.message.orEmpty()
        when {
            msg.contains("HTTP 5", ignoreCase = true) -> ApiError.Server(cause = error)
            msg.contains("HTTP 401", ignoreCase = true) || msg.contains("HTTP 403", ignoreCase = true) ->
                ApiError.Unauthorized(cause = error)
            msg.contains("HTTP 409", ignoreCase = true) -> ApiError.Conflict(cause = error)
            msg.contains("HTTP 429", ignoreCase = true) -> ApiError.RateLimit(cause = error)
            isHumanFriendly(msg) -> ApiError.Client(message = msg, cause = error)
            else -> ApiError.Unknown(cause = error)
        }
    }
}

/** Determines if a message is suitable for customer display vs internal exception jargon. */
private fun isHumanFriendly(message: String): Boolean {
    val trimmed = message.trim()
    if (trimmed.isBlank() || trimmed.length > 200) return false
    val technicalMarkers = listOf(
        "Exception", "Error:", "NullPointer", "retrofit2.", "okhttp3.",
        "java.", "android.", "kotlin.", "Stack trace", "at com.", "org.json.",
        "{", "}", "JSON", "FATAL",
    )
    return technicalMarkers.none { trimmed.contains(it, ignoreCase = false) }
}

/** Formats an OTP dispatch success message for mobile display. */
fun formatOtpSuccessMessage(phone: String): String {
    val clean = phone.replace(Regex("[^0-9]"), "")
    val digits = if (clean.startsWith("91") && clean.length == 12) clean.drop(2) else clean
    val formattedPhone = when {
        digits.length == 10 -> "+91 ${digits.take(5)} ${digits.takeLast(5)}"
        digits.isNotEmpty() -> "+91 $digits"
        else -> "your mobile number"
    }
    return "Verification code sent to $formattedPhone"
}
