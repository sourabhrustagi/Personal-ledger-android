package com.ledger.personal.data.remote.api

import com.ledger.personal.core.LocalBillPay
import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.domain.pay.BillPay
import com.ledger.personal.domain.pay.BillPayResponse
import com.ledger.personal.domain.pay.FetchedBill
import com.ledger.personal.domain.pay.SealedPay
import com.ledger.personal.di.ApiConfig
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Serializable
data class PayKeyDto(val publicKey: String = "")

@Serializable
data class SealedPayDto(val clientPublic: String, val iv: String, val body: String)

@Serializable
data class FetchedBillDto(val billNumber: String = "", val amountMinor: Long = 0, val holder: String = "")

@Serializable
data class BillPayResponseDto(val status: String = "SUCCESS", val message: String = "Bill paid successfully") {
    fun toDomain(): BillPayResponse = BillPayResponse(status = status, message = message)
}

/** Retrofit API contract for bill payments. */
interface BillPayRetrofitApi {
    @GET(ApiEndpoints.BILL_PAY_KEY)
    suspend fun getKey(): PayKeyDto

    @POST(ApiEndpoints.BILLS_LOOKUP)
    suspend fun lookup(@Body request: SealedPayDto): FetchedBillDto

    @POST(ApiEndpoints.BILLS_PAY)
    suspend fun pay(@Body request: SealedPayDto): BillPayResponseDto
}

/** Mock decrypts locally. Live fetches the server public key, then posts the sealed payment via Retrofit. */
@Singleton
class InterceptorBillPay @Inject constructor(
    private val config: ApiConfig,
    private val api: BillPayRetrofitApi,
) : BillPay {
    private val local = LocalBillPay()

    override suspend fun serverPublic(): ByteArray = if (config.mockFlavor) {
        local.serverPublic()
    } else {
        Base64.getDecoder().decode(api.getKey().publicKey)
    }

    override suspend fun fetch(sealed: SealedPay): Result<FetchedBill> = if (config.mockFlavor) {
        local.fetch(sealed)
    } else {
        runCatching {
            val posted = api.lookup(sealed.toDto())
            FetchedBill(posted.billNumber, posted.amountMinor, posted.holder)
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun submit(sealed: SealedPay): Result<BillPayResponse> = if (config.mockFlavor) {
        local.submit(sealed)
    } else {
        runCatching {
            val response = api.pay(sealed.toDto())
            require(response.status == "SUCCESS") { response.message.ifBlank { "Payment failed" } }
            response.toDomain()
        }.onFailure { if (it is CancellationException) throw it }
    }
}

private fun SealedPay.toDto(): SealedPayDto {
    val b64 = Base64.getEncoder()
    return SealedPayDto(
        b64.encodeToString(clientPublic),
        b64.encodeToString(iv),
        b64.encodeToString(body),
    )
}

