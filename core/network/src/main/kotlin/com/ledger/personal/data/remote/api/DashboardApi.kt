package com.ledger.personal.data.remote.api

import com.ledger.personal.data.remote.ApiEndpoints
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeLayout
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.HomeTile
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

/** Retrofit API contract for dashboard layout. */
interface DashboardRetrofitApi {
    @GET(ApiEndpoints.DASHBOARD)
    suspend fun getDashboard(): DashboardDto
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
