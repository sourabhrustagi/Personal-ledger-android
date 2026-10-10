package com.ledger.personal.domain.board

/** One home shortcut. The dashboard team chooses the id and the title. */
data class HomeTile(val id: String, val title: String)

/** Layout of the home screen, owned by the app team's dashboard API. */
interface HomeBoard {
    /** Returns tiles. */
    suspend fun tiles(): Result<List<HomeTile>>
}

/** A person the payments team says you can pay. */
data class PayShortcut(val name: String, val handle: String)

/** Payments backend. Its JSON is not shared with the other teams. */
interface PaymentsBackend {
    /** Returns shortcuts. */
    suspend fun shortcuts(): Result<List<PayShortcut>>
}

/** A spending line from the insights team. */
data class SpendHighlight(val label: String, val spentMinor: Long)

/** Insights backend. Amounts arrive in that team's own field, already in minor units. */
interface InsightsBackend {
    /** Returns highlights. */
    suspend fun highlights(): Result<List<SpendHighlight>>
}

/** Assistant backend. Prompts are plain strings from that team's endpoint. */
interface AssistantBackend {
    /** Returns prompts. */
    suspend fun prompts(): Result<List<String>>
}

/** One deal on a promotions shelf. */
data class PromoOffer(val id: String, val pitch: String, val until: String)

/** One tab of promotions. The promotions team chooses the id and the title. */
data class PromoTab(val id: String, val title: String, val offers: List<PromoOffer>)

/** Promotions backend. Shelves are that team's own JSON, not the dashboard tiles. */
interface PromotionsBackend {
    /** Returns tabs. */
    suspend fun tabs(): Result<List<PromoTab>>
}
