package com.ledger.personal.data.remote.api

import com.ledger.personal.di.AuthenticatedClient
import com.ledger.personal.di.ApiConfig
import com.ledger.personal.di.TeamClient
import com.ledger.personal.domain.board.AssistantBackend
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
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable
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


/** Assistant payload. */
@Serializable
data class AssistantDto(val prompts: List<String> = emptyList())

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

/** Reads the home layout from the app team's dashboard. */
@Singleton
class KtorHomeBoard @Inject constructor(
    @param:AuthenticatedClient private val client: HttpClient,
) : HomeBoard {
    /** Returns layout. */
    override suspend fun layout(): Result<HomeLayout> = runCatching {
        client.get("dashboard").body<DashboardDto>().toLayout()
    }.onFailure { if (it is CancellationException) throw it }
}

/** Reads payees from the payments host. */
@Singleton
class KtorPaymentsBackend @Inject constructor(
    @param:TeamClient private val client: HttpClient,
    private val config: ApiConfig,
) : PaymentsBackend {
    /** Returns shortcuts. */
    override suspend fun shortcuts(): Result<List<PayShortcut>> = runCatching {
        client.get("${config.paymentsBaseUrl}pay/shortcuts").body<PaymentsDto>().payees.mapNotNull { it.toShortcut() }
    }.onFailure { if (it is CancellationException) throw it }
}


/** Reads starter prompts from the assistant host. */
@Singleton
class KtorAssistantBackend @Inject constructor(
    @param:TeamClient private val client: HttpClient,
    private val config: ApiConfig,
) : AssistantBackend {
    /** Returns prompts. */
    override suspend fun prompts(): Result<List<String>> = runCatching {
        client.get("${config.assistantBaseUrl}chat/starters").body<AssistantDto>().prompts.filter { it.isNotBlank() }
    }.onFailure { if (it is CancellationException) throw it }
}

/** Reads shelves from the promotions host. */
@Singleton
class KtorPromotionsBackend @Inject constructor(
    @param:TeamClient private val client: HttpClient,
    private val config: ApiConfig,
) : PromotionsBackend {
    /** Returns tabs. */
    override suspend fun tabs(): Result<List<PromoTab>> = runCatching {
        client.get("${config.promotionsBaseUrl}campaigns/shelves").body<PromotionsDto>().shelves.mapNotNull { it.toTab() }
    }.onFailure { if (it is CancellationException) throw it }
}
