package com.ledger.personal.feature.auth

import com.ledger.personal.domain.session.OtpRepository
import com.ledger.personal.domain.session.OtpResponse
import com.ledger.personal.domain.usecase.SendOtpUseCase
import com.ledger.personal.mock.MockValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelsTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun otpUiStateDefaultValues() {
        val state = OtpUiState()
        assertFalse(state.isSending)
        assertFalse(state.isOtpSent)
        assertEquals("OTP sent", state.sentMessage)
        assertNull(state.errorMessage)
        assertEquals(0, state.resendCountdown)
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

    @Test
    fun otpConstantsMatchExpectedValues() {
        assertEquals(30, AppConstants.OTP_RESEND_COOLDOWN_SECONDS)
        assertEquals(6, AppConstants.OTP_CODE_LENGTH)
        assertEquals(AppConstants.OTP_RESEND_COOLDOWN_SECONDS, OnboardingViewModel.RESEND_SECONDS)
    }

    @Test
    fun onboardingViewModelInitialStateHasZeroCountdown() {
        val viewModel = OnboardingViewModel(
            sendOtpUseCase = SendOtpUseCase(object : OtpRepository {
                override suspend fun sendOtp(phone: String): Result<OtpResponse> =
                    Result.success(OtpResponse(message = "OTP sent"))
            }),
        )
        assertEquals(0, viewModel.resendCountdown.value)
        assertEquals(0, viewModel.otpState.value.resendCountdown)
    }

    @Test
    fun startResendTimerTicksDownEverySecond() = runTest {
        val viewModel = OnboardingViewModel(
            sendOtpUseCase = SendOtpUseCase(object : OtpRepository {
                override suspend fun sendOtp(phone: String): Result<OtpResponse> =
                    Result.success(OtpResponse(message = "OTP sent"))
            }),
        )

        viewModel.startResendTimer(3)
        testDispatcher.scheduler.runCurrent()
        assertEquals(3, viewModel.resendCountdown.value)
        assertEquals(3, viewModel.otpState.value.resendCountdown)

        advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.resendCountdown.value)

        advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.resendCountdown.value)

        advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(0, viewModel.resendCountdown.value)
    }

    @Test
    fun sendOtpSuccessStartsCountdownTimer() = runTest {
        val fakeRepo = object : OtpRepository {
            override suspend fun sendOtp(phone: String): Result<OtpResponse> =
                Result.success(OtpResponse(message = "Code dispatched"))
        }
        val viewModel = OnboardingViewModel(sendOtpUseCase = SendOtpUseCase(fakeRepo))

        viewModel.sendOtp("9876543210")
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.otpState.value.isOtpSent)
        assertEquals("Code dispatched", viewModel.otpState.value.sentMessage)
        assertEquals(OnboardingViewModel.RESEND_SECONDS, viewModel.resendCountdown.value)

        // Advance 1 second and verify countdown decrements
        advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(OnboardingViewModel.RESEND_SECONDS - 1, viewModel.resendCountdown.value)
    }

    @Test
    fun sendOtpFailureDoesNotStartCountdownTimer() = runTest {
        val fakeRepo = object : OtpRepository {
            override suspend fun sendOtp(phone: String): Result<OtpResponse> =
                Result.failure(IllegalArgumentException("Network error"))
        }
        val viewModel = OnboardingViewModel(sendOtpUseCase = SendOtpUseCase(fakeRepo))

        viewModel.sendOtp("9876543210")
        testDispatcher.scheduler.runCurrent()

        assertFalse(viewModel.otpState.value.isOtpSent)
        assertEquals("Network error", viewModel.otpState.value.errorMessage)
        assertEquals(0, viewModel.resendCountdown.value)
    }

    @Test
    fun loginViewModelInitialStateHasDefaultCountdown() {
        val viewModel = LoginViewModel()
        testDispatcher.scheduler.runCurrent()
        assertEquals(AppConstants.OTP_RESEND_COOLDOWN_SECONDS, viewModel.resendCountdown.value)
    }

    @Test
    fun loginViewModelTimerTicksDown() = runTest {
        val viewModel = LoginViewModel()
        viewModel.startResendTimer(2)
        testDispatcher.scheduler.runCurrent()
        assertEquals(2, viewModel.resendCountdown.value)

        advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(1, viewModel.resendCountdown.value)

        advanceTimeBy(1000)
        testDispatcher.scheduler.runCurrent()
        assertEquals(0, viewModel.resendCountdown.value)
    }

    @Test
    fun loginViewModelVerifyOtpSuccessAndFailure() {
        val viewModel = LoginViewModel()
        var successResult: Boolean? = null
        viewModel.verifyOtp(MockValues.OTP) { successResult = it }
        testDispatcher.scheduler.runCurrent()
        assertEquals(true, successResult)

        var failResult: Boolean? = null
        viewModel.verifyOtp("000000") { failResult = it }
        testDispatcher.scheduler.runCurrent()
        assertEquals(false, failResult)
    }

    @Test
    fun onboardingViewModelClickHandlersUpdateState() = runTest {
        val fakeRepo = object : OtpRepository {
            override suspend fun sendOtp(phone: String): Result<OtpResponse> =
                Result.success(OtpResponse(message = "OTP dispatched"))
        }
        val viewModel = OnboardingViewModel(sendOtpUseCase = SendOtpUseCase(fakeRepo))

        viewModel.onSendOtpClick("9876543210")
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.otpState.value.isOtpSent)
        assertEquals("9876543210", viewModel.otpState.value.phone)

        // Test edit phone click resets isOtpSent to return to phone input step
        viewModel.onEditPhoneClick()
        assertFalse(viewModel.otpState.value.isOtpSent)

        // Test verify OTP failure
        var completed = false
        viewModel.onVerifyOtpClick("999999", "9876543210") { completed = true }
        testDispatcher.scheduler.runCurrent()
        assertTrue(viewModel.otpState.value.isOtpError)
        assertFalse(completed)

        // Test dismiss error
        viewModel.dismissOtpError()
        assertFalse(viewModel.otpState.value.isOtpError)

        // Test verify OTP success
        viewModel.onVerifyOtpClick(MockValues.OTP, "9876543210") { completed = true }
        testDispatcher.scheduler.runCurrent()
        assertFalse(viewModel.otpState.value.isOtpError)
        assertTrue(completed)
    }
}
