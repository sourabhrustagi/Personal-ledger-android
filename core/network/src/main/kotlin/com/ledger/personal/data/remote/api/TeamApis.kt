package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeLayout
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.HomeTile
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PaymentsBackend
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionsBackend
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

/** Dashboard payload. Tabs and sections are drawn in this order. Unknown fields are ignored. */
@Serializable
data class DashboardDto(
    val tabs: List<TabDto> = emptyList(),
    val sections: List<SectionDto> = emptyList(),
)

/** One bottom tab as the dashboard team sends it. */
@Serializable
data class TabDto(val id: String = "", val title: String = "")

/** One home block. [kind] picks the component. Extra fields for other kinds are ignored. */
@Serializable
data class SectionDto(
    val kind: String = "",
    val title: String = "",
    val body: String = "",
    val items: List<TileDto> = emptyList(),
)

/** One shortcut as the dashboard team sends it. */
@Serializable
data class TileDto(val id: String = "", val title: String = "")

/** Payments payload. Names differ from the dashboard on purpose. */
@Serializable
data class PaymentsDto(val payees: List<PayeeDto> = emptyList())

/** One payee as the payments team sends it. */
@Serializable
data class PayeeDto(val displayName: String = "", val vpa: String = "")

/** Promotions payload. Field names are this team's, not the dashboard's. */
@Serializable
data class PromotionsDto(val shelves: List<ShelfDto> = emptyList())

/** One shelf of deals. */
@Serializable
data class ShelfDto(val key: String = "", val name: String = "", val offers: List<OfferDto> = emptyList())

/** One deal as the promotions team sends it. */
@Serializable
data class OfferDto(val sku: String = "", val pitch: String = "", val until: String = "")

/** Drops a tab the dashboard left blank. */
fun TabDto.toTab(): HomeTab? = if (id.isBlank() || title.isBlank()) null else HomeTab(id, title)

/** Drops a tile the dashboard left blank. */
fun TileDto.toHomeTile(): HomeTile? = if (id.isBlank() || title.isBlank()) null else HomeTile(id, title)

/** Keeps the tabs and sections the dashboard actually filled in. */
fun DashboardDto.toLayout(): HomeLayout = HomeLayout(
    tabs.mapNotNull { it.toTab() },
    sections.mapNotNull { it.toSection() },
)

/** Turns one dashboard row into a home component. An unknown kind is skipped. */
fun SectionDto.toSection(): HomeSection? = when (kind) {
    "balance" -> HomeSection.Balance
    "shortcuts" -> items.mapNotNull { it.toHomeTile() }.takeIf { it.isNotEmpty() }?.let { HomeSection.Shortcuts(it) }
    "note" -> if (body.isBlank()) null else HomeSection.Note(title, body)
    "activity" -> HomeSection.Activity(title.ifBlank { "Recent" })
    else -> null
}

/** Drops a payee with no name or handle. */
fun PayeeDto.toShortcut(): PayShortcut? =
    if (displayName.isBlank() || vpa.isBlank()) null else PayShortcut(displayName, vpa)

/** Drops a deal with no id or pitch. */
fun OfferDto.toOffer(): PromoOffer? = if (sku.isBlank() || pitch.isBlank()) null else PromoOffer(sku, pitch, until)

/** Drops a shelf with no key or name, and drops blank deals inside it. */
fun ShelfDto.toTab(): PromoTab? {
    if (key.isBlank() || name.isBlank()) return null
    return PromoTab(key, name, offers.mapNotNull { it.toOffer() })
}

/** Retrofit API contract for dashboard layout. */
interface DashboardRetrofitApi {
    @GET(ApiEndpoints.DASHBOARD)
    suspend fun getDashboard(): DashboardDto
}

/** Retrofit API contract for payment shortcuts. */
interface PaymentsRetrofitApi {
    @GET(ApiEndpoints.PAY_SHORTCUTS)
    suspend fun getPayShortcuts(): PaymentsDto
}

/** Retrofit API contract for promotional campaigns. */
interface PromotionsRetrofitApi {
    @GET(ApiEndpoints.CAMPAIGN_SHELVES)
    suspend fun getCampaignShelves(): PromotionsDto
}

/** Reads the home layout from the app team's dashboard. */
@Singleton
class RetrofitHomeBoard @Inject constructor(
    private val api: DashboardRetrofitApi,
) : HomeBoard {
    /** Returns layout. */
    override suspend fun layout(): Result<HomeLayout> = runCatching {
        api.getDashboard().toLayout()
    }.onFailure { if (it is CancellationException) throw it }
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

/** Backward-compatible typealias for [RetrofitHomeBoard]. */
typealias KtorHomeBoard = RetrofitHomeBoard

/** Backward-compatible typealias for [RetrofitPaymentsBackend]. */
typealias KtorPaymentsBackend = RetrofitPaymentsBackend

/** Backward-compatible typealias for [RetrofitPromotionsBackend]. */
typealias KtorPromotionsBackend = RetrofitPromotionsBackend

