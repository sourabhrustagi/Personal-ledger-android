package com.ledger.personal.domain.board

/** One bottom tab. The dashboard team chooses the id and the title. */
data class HomeTab(val id: String, val title: String)

/** One home shortcut. The dashboard team chooses the id and the title. */
data class HomeTile(val id: String, val title: String)

/** Tabs and home blocks from one dashboard response. */
data class HomeLayout(val tabs: List<HomeTab>, val sections: List<HomeSection>)

/** One block on the home screen. The dashboard chooses the kind and the order. */
sealed interface HomeSection {
    /** Savings balance and this month's totals. */
    data object Balance : HomeSection

    /** Shortcut row. An empty item list is not a section. */
    data class Shortcuts(val tiles: List<HomeTile>) : HomeSection

    /** A card whose copy comes from the dashboard. */
    data class Note(val title: String, val body: String) : HomeSection

    /** Search, category chips, and recent transactions. */
    data class Activity(val title: String) : HomeSection
}

/** Layout of the home screen, owned by the app team's dashboard API. */
fun interface HomeBoard {
    /** Returns the tabs and the home blocks in the order the dashboard wants them drawn. */
    suspend fun layout(): Result<HomeLayout>
}
