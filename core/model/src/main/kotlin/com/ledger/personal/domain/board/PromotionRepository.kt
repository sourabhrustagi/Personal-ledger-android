package com.ledger.personal.domain.board

import kotlinx.coroutines.flow.Flow

/** Contract for observing promotions. */
interface PromotionRepository {
    /** Streams promotion tabs. */
    fun observePromotions(): Flow<List<PromoTab>>
}
