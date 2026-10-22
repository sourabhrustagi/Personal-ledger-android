package com.ledger.personal.domain.board

/** One deal on a promotions shelf. */
data class PromoOffer(val id: String, val pitch: String, val until: String)

/** One tab of promotions. The promotions team chooses the id and the title. */
data class PromoTab(val id: String, val title: String, val offers: List<PromoOffer>)

/** Promotions backend. Shelves are that team's own JSON, not the dashboard tiles. */
fun interface PromotionsBackend {
    /** Returns tabs. */
    suspend fun tabs(): Result<List<PromoTab>>
}
