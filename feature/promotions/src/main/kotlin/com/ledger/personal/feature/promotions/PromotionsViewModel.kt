package com.ledger.personal.feature.promotions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.usecase.GetPromotionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** UI State for the Promotions screen. */
data class PromotionsUiState(
    val shelves: List<PromoTab> = emptyList(),
    val selected: String = "",
)

/** Promotions view model, consuming [GetPromotionsUseCase]. */
@HiltViewModel
class PromotionsViewModel @Inject constructor(
    getPromotionsUseCase: GetPromotionsUseCase,
    handle: SavedStateHandle? = null,
) : ViewModel() {
    private val requested = handle?.get<String>("tab").orEmpty()
    private val picked = MutableStateFlow(requested)
    val shelves: StateFlow<List<PromoTab>> = getPromotionsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val selected: StateFlow<String> = picked.asStateFlow()

    val uiState: StateFlow<PromotionsUiState> = combine(shelves, selected) { shelfList, sel ->
        val active = if (sel.isNotBlank()) sel else shelfList.firstOrNull()?.id.orEmpty()
        PromotionsUiState(shelves = shelfList, selected = active)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PromotionsUiState())

    /** Selects a shelf by its key. */
    fun select(id: String) {
        picked.value = id
    }
}
