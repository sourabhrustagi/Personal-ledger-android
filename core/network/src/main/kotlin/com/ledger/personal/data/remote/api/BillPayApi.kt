package com.ledger.personal.data.remote.api

import com.ledger.personal.core.LocalBillPay
import com.ledger.personal.domain.pay.BillPay
import com.ledger.personal.domain.pay.FetchedBill
import com.ledger.personal.domain.pay.SealedPay
import com.ledger.personal.di.ApiConfig
import com.ledger.personal.di.AuthenticatedClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.Serializable
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

/** Mock decrypts locally. Live fetches the server public key, then posts the sealed payment. */
@Singleton
class InterceptorBillPay @Inject constructor(
    private val config: ApiConfig,
    @param:AuthenticatedClient private val client: HttpClient,
) : BillPay {
    private val local = LocalBillPay()

    override suspend fun serverPublic(): ByteArray = if (config.localSession) {
        local.serverPublic()
    } else {
        Base64.getDecoder().decode(client.get("pay/key").body<PayKeyDto>().publicKey)
    }

    override suspend fun fetch(sealed: SealedPay): Result<FetchedBill> = if (config.localSession) {
        local.fetch(sealed)
    } else {
        runCatching {
            val posted = client.post("bills/lookup") { setBody(sealed.toDto()) }.body<FetchedBillDto>()
            FetchedBill(posted.billNumber, posted.amountMinor, posted.holder)
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun submit(sealed: SealedPay): Result<Unit> = if (config.localSession) {
        local.submit(sealed)
    } else {
        runCatching {
            client.post("bills/pay") { setBody(sealed.toDto()) }
            Unit
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
