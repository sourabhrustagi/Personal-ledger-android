package com.ledger.personal.domain.usecase

import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe promotion tabs.
 */
class GetPromotionsUseCase @Inject constructor(
    private val repository: PromotionRepository,
) {
    operator fun invoke(): Flow<List<PromoTab>> = repository.observePromotions()
}
