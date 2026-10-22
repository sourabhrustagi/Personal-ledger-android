package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PaymentsBackend
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Payments payload. Names differ from the dashboard on purpose. */
@Serializable
data class PaymentsDto(val payees: List<PayeeDto> = emptyList())

/** One payee as the payments team sends it. */
@Serializable
data class PayeeDto(val displayName: String = "", val vpa: String = "")

/** Drops a payee with no name or handle. */
fun PayeeDto.toShortcut(): PayShortcut? =
    if (displayName.isBlank() || vpa.isBlank()) null else PayShortcut(displayName, vpa)

/** Retrofit API contract for payment shortcuts. */
interface PaymentsRetrofitApi {
    @GET(ApiEndpoints.PAY_SHORTCUTS)
    suspend fun getPayShortcuts(): PaymentsDto
}

/** Reads payees from the payments host. */
@Singleton
class RetrofitPaymentsBackend @Inject constructor(
    private val api: PaymentsRetrofitApi,
) : PaymentsBackend {
    /** Returns shortcuts. */
    override suspend fun shortcuts(): Result<List<PayShortcut>> = runCatching {
        api.getPayShortcuts().payees.mapNotNull { it.toShortcut() }
    }.onFailure { if (it is CancellationException) throw it }
}
