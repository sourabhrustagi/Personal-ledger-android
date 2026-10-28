package com.ledger.personal.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ApiErrorTest {

    @Test
    fun formatsNetworkErrorsAsNoConnection() {
        val error = UnknownHostException("Unable to resolve host \"api.ledger.internal\": No address associated")
        val formatted = formatApiError(error)
        assertEquals("No internet connection. Please check your network and try again.", formatted)

        val apiError = toApiError(ConnectException("Failed to connect to 127.0.0.1"))
        assertTrue(apiError is ApiError.Network)
        assertTrue(apiError.isRetriable)
    }

    @Test
    fun formatsTimeoutErrorsAsConnectionTimedOut() {
        val error = SocketTimeoutException("timeout")
        val formatted = formatApiError(error)
        assertEquals("The server took too long to respond. Please try again.", formatted)

        val apiError = toApiError(error)
        assertTrue(apiError is ApiError.Timeout)
        assertTrue(apiError.isRetriable)
    }

    @Test
    fun formatsServerBusyAsServiceUnavailable() {
        val error = ServerBusy("Bank busy")
        val apiError = toApiError(error)
        assertTrue(apiError is ApiError.Server)
        assertEquals("Service unavailable", apiError.title)
    }

    @Test
    fun preservesHumanFriendlyValidationMessages() {
        val error = IllegalArgumentException("Invalid 10-digit phone number")
        val formatted = formatApiError(error)
        assertEquals("Invalid 10-digit phone number", formatted)
    }

    @Test
    fun sanitizesRawTechnicalExceptions() {
        val error = NullPointerException("java.lang.NullPointerException at com.ledger.personal.Data.get()")
        val formatted = formatApiError(error)
        assertEquals("An unexpected error occurred. Please try again.", formatted)
    }

    @Test
    fun formatsRawHttpErrorStrings() {
        assertEquals(
            "Our servers are currently unavailable. Please try again shortly.",
            formatApiError("HTTP 500 Internal Server Error"),
        )
        assertEquals(
            "Your session has expired. Please sign in again.",
            formatApiError("HTTP 401 Unauthorized"),
        )
        assertEquals(
            "Too many attempts. Please wait a moment before trying again.",
            formatApiError("HTTP 429 Too Many Requests"),
        )
    }

    @Test
    fun formatsOtpSuccessMessageProperly() {
        assertEquals(
            "Verification code sent to +91 98765 43210",
            formatOtpSuccessMessage("9876543210"),
        )
        assertEquals(
            "Verification code sent to +91 98765 43210",
            formatOtpSuccessMessage("+919876543210"),
        )
        assertEquals(
            "Verification code sent to your mobile number",
            formatOtpSuccessMessage(""),
        )
    }
}
