package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionsBackend
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Promotions payload. Field names are this team's, not the dashboard's. */
@Serializable
data class PromotionsDto(val shelves: List<ShelfDto> = emptyList())

/** One shelf of deals. */
@Serializable
data class ShelfDto(val key: String = "", val name: String = "", val offers: List<OfferDto> = emptyList())

/** One deal as the promotions team sends it. */
@Serializable
data class OfferDto(val sku: String = "", val pitch: String = "", val until: String = "")

/** Drops a deal with no id or pitch. */
fun OfferDto.toOffer(): PromoOffer? = if (sku.isBlank() || pitch.isBlank()) null else PromoOffer(sku, pitch, until)

/** Drops a shelf with no key or name, and drops blank deals inside it. */
fun ShelfDto.toTab(): PromoTab? {
    if (key.isBlank() || name.isBlank()) return null
    return PromoTab(key, name, offers.mapNotNull { it.toOffer() })
}

/** Retrofit API contract for promotional campaigns. */
interface PromotionsRetrofitApi {
    @GET(ApiEndpoints.CAMPAIGN_SHELVES)
    suspend fun getCampaignShelves(): PromotionsDto
}

/** Reads shelves from the promotions host. */
@Singleton
class RetrofitPromotionsBackend @Inject constructor(
    private val api: PromotionsRetrofitApi,
) : PromotionsBackend {
    /** Returns tabs. */
    override suspend fun tabs(): Result<List<PromoTab>> = runCatching {
        api.getCampaignShelves().shelves.mapNotNull { it.toTab() }
    }.onFailure { if (it is CancellationException) throw it }
}

/** Backward-compatible typealias for [RetrofitPromotionsBackend]. */
typealias KtorPromotionsBackend = RetrofitPromotionsBackend
