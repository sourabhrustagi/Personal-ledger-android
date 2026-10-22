package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.mock.MockResponses
import com.ledger.personal.di.ApiConfig
import com.ledger.personal.domain.pay.SealedPay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BillPayApiTest {

    private val config = ApiConfig(
        baseUrl = "https://api.example/retail/v1/",
        logRequests = false,
        debuggable = false,
        pinSha256 = "",
        pinBackup = "",
        paymentsBaseUrl = "",
        promotionsBaseUrl = "",
        mockFlavor = false,
    )

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun liveSubmitParsesBillPayResponse() = runBlocking {
        val retrofitApi = object : BillPayRetrofitApi {
            override suspend fun getKey(): PayKeyDto = PayKeyDto("key")
            override suspend fun lookup(request: SealedPayDto): FetchedBillDto = FetchedBillDto()
            override suspend fun pay(request: SealedPayDto): BillPayResponseDto =
                json.decodeFromString(BillPayResponseDto.serializer(), MockResponses.BILLS_PAY)
        }
        val api = InterceptorBillPay(config, retrofitApi)
        val dummy = SealedPay(ByteArray(32), ByteArray(12), ByteArray(16))
        val result = api.submit(dummy)
        assertTrue(result.isSuccess)
        val response = result.getOrNull()
        assertEquals("SUCCESS", response?.status)
        assertEquals("Bill paid successfully", response?.message)
    }
}
