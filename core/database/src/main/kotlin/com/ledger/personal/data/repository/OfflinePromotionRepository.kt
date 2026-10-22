package com.ledger.personal.data.repository

import com.ledger.personal.data.local.PromotionDao
import com.ledger.personal.data.local.PromotionEntity
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Offline-first repository for promotions, backed by Room [PromotionDao].
 */
class OfflinePromotionRepository @Inject constructor(
    private val dao: PromotionDao,
) : PromotionRepository {

    override fun observePromotions(): Flow<List<PromoTab>> =
        dao.observe().map(::shelvesOf)

    private fun shelvesOf(rows: List<PromotionEntity>): List<PromoTab> =
        rows.groupBy { it.shelfId }.values.map { shelf ->
            val first = shelf.first()
            PromoTab(
                id = first.shelfId,
                title = first.shelfTitle,
                offers = shelf.map { PromoOffer(it.id, it.pitch, it.until) },
            )
        }
}
