package com.ledger.personal.feature.promotions.model

import com.ledger.personal.domain.board.PromoTab

/** State for promotions screen. */
data class PromotionsUiState(
    val shelves: List<PromoTab> = emptyList(),
    val selected: String = "",
)

