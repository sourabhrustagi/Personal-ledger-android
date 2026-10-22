package com.ledger.personal.ui.app.auth

import com.ledger.personal.mock.MockValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthViewModelsTest {

    @Test
    fun otpUiStateDefaultValues() {
        val state = OtpUiState()
        assertFalse(state.isSending)
        assertFalse(state.isOtpSent)
        assertEquals("OTP sent", state.sentMessage)
        assertNull(state.errorMessage)
    }

    @Test
    fun otpUiStateCopiesCorrectlyOnSending() {
        val initial = OtpUiState()
        val sending = initial.copy(isSending = true, errorMessage = null)
        assertTrue(sending.isSending)
        assertFalse(sending.isOtpSent)
        assertNull(sending.errorMessage)
    }

    @Test
    fun otpUiStateCopiesCorrectlyOnSuccess() {
        val success = OtpUiState().copy(
            isSending = false,
            isOtpSent = true,
            sentMessage = "OTP sent successfully",
        )
        assertFalse(success.isSending)
        assertTrue(success.isOtpSent)
        assertEquals("OTP sent successfully", success.sentMessage)
        assertNull(success.errorMessage)
    }

    @Test
    fun otpUiStateCopiesCorrectlyOnFailure() {
        val failure = OtpUiState().copy(
            isSending = false,
            isOtpSent = false,
            errorMessage = "Invalid phone number",
        )
        assertFalse(failure.isSending)
        assertFalse(failure.isOtpSent)
        assertEquals("Invalid phone number", failure.errorMessage)
    }

    @Test
    fun mockValuesMatchOtpRequirements() {
        assertEquals("123456", MockValues.OTP)
        assertEquals("1111", MockValues.MPIN)
    }
}
