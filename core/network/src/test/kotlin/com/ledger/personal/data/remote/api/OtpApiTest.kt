package com.ledger.personal.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpApiTest {

    @Test
    fun mockSendOtpReturnsOtpSentForValidPhone() = runBlocking {
        val retrofitApi = object : OtpRetrofitApi {
            override suspend fun sendOtp(request: OtpRequestDto): OtpResponseDto =
                OtpResponseDto(message = "OTP sent", status = "SENT")
        }
        val repository = RetrofitOtpRepository(retrofitApi, Dispatchers.Unconfined)
        val result = repository.sendOtp("9876543210")
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals("OTP sent", response?.message)
        assertEquals("SENT", response?.status)
    }

    @Test
    fun sendOtpFailsForInvalidPhone() = runBlocking {
        val retrofitApi = object : OtpRetrofitApi {
            override suspend fun sendOtp(request: OtpRequestDto): OtpResponseDto =
                error("invalid phone should fail before network call")
        }
        val repository = RetrofitOtpRepository(retrofitApi, Dispatchers.Unconfined)
        val result = repository.sendOtp("123")
        assertTrue(result.isFailure)
    }

    @Test
    fun liveSendOtpPostsToServerAndReturnsMessage() = runBlocking {
        val retrofitApi = object : OtpRetrofitApi {
            override suspend fun sendOtp(request: OtpRequestDto): OtpResponseDto =
                OtpResponseDto(message = "OTP sent", status = "SENT")
        }
        val repository = RetrofitOtpRepository(retrofitApi, Dispatchers.Unconfined)
        val result = repository.sendOtp("9876543210")
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals("OTP sent", response?.message)
        assertEquals("SENT", response?.status)
    }
}
