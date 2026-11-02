package com.ledger.personal.feature.home.model

import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.sync.SyncPhase
import com.ledger.personal.domain.model.Transaction

/** Home and main product tabs stay reachable when the dashboard cannot be loaded. */
val essentialTabs = listOf(
    HomeTab("home", "Home"),
    HomeTab("transfers", "Pay"),
    HomeTab("history", "Passbook"),
    HomeTab("account", "Account"),
)

/** Values rendered by the home screen. */
data class HomeUiState(
    val loading: Boolean = true,
    val query: String = "",
    val category: String? = null,
    val categories: List<String> = emptyList(),
    val groups: List<DayGroup> = emptyList(),
    val monthInMinor: Long = 0,
    val monthOutMinor: Long = 0,
    val sync: SyncPhase = SyncPhase.IDLE,
    val pendingDelete: Transaction? = null,
    val total: Int = 0,
    val tabs: List<HomeTab> = essentialTabs,
    val sections: List<HomeSection> = emptyList(),
    val layoutReady: Boolean = false,
    val layoutMessage: String? = null,
    val promotions: List<PromoOffer> = emptyList(),
)

