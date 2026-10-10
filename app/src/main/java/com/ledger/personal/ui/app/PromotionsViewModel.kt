package com.ledger.personal.ui.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ledger.personal.data.local.PromotionDao
import com.ledger.personal.data.local.PromotionEntity
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Promotions read from Room, with the deep-linked tab selected when it exists. */
@HiltViewModel
class PromotionsViewModel @Inject constructor(
    dao: PromotionDao,
    handle: SavedStateHandle,
) : ViewModel() {
    private val requested = handle.toRoute<Screen.Promotions>().tab
    private val picked = MutableStateFlow(requested)
    val shelves: StateFlow<List<PromoTab>> = dao.observe()
        .map(::shelvesOf)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selected: StateFlow<String> = picked.asStateFlow()

    /** Selects a shelf by its key. */
    fun select(id: String) {
        picked.value = id
    }

    /** Groups rows into shelves. The query order is the shelf order. */
    private fun shelvesOf(rows: List<PromotionEntity>): List<PromoTab> =
        rows.groupBy { it.shelfId }.values.map { shelf ->
            val first = shelf.first()
            PromoTab(first.shelfId, first.shelfTitle, shelf.map { PromoOffer(it.id, it.pitch, it.until) })
        }
}
