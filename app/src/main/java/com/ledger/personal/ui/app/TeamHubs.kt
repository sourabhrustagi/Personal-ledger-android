package com.ledger.personal.ui.app

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.ledger.personal.domain.board.InsightsBackend
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PaymentsBackend
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.board.SpendHighlight
import com.ledger.personal.ui.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Payees loaded from the payments team's endpoint. */
@HiltViewModel
class PayHubViewModel @Inject constructor(payments: PaymentsBackend) : ViewModel() {
    private val state = MutableStateFlow<Result<List<PayShortcut>>?>(null)
    val shortcuts: StateFlow<Result<List<PayShortcut>>?> = state.asStateFlow()

    init {
        viewModelScope.launch { state.value = payments.shortcuts() }
    }
}

/** Spending lines loaded from the insights team's endpoint. */
@HiltViewModel
class InsightsHubViewModel @Inject constructor(insights: InsightsBackend) : ViewModel() {
    private val state = MutableStateFlow<Result<List<SpendHighlight>>?>(null)
    val highlights: StateFlow<Result<List<SpendHighlight>>?> = state.asStateFlow()

    init {
        viewModelScope.launch { state.value = insights.highlights() }
    }
}

/** Shelves from the promotions team, with the deep-linked tab selected when it exists. */
@HiltViewModel
class PromotionsViewModel @Inject constructor(
    promotions: PromotionsBackend,
    handle: SavedStateHandle,
) : ViewModel() {
    private val requested = handle.toRoute<Screen.Promotions>().tab
    private val loaded = MutableStateFlow<Result<List<PromoTab>>?>(null)
    private val picked = MutableStateFlow(requested)
    val shelves: StateFlow<Result<List<PromoTab>>?> = loaded.asStateFlow()
    val selected: StateFlow<String> = picked.asStateFlow()

    init {
        viewModelScope.launch { loaded.value = promotions.tabs() }
    }

    /** Selects a shelf by its key. */
    fun select(id: String) {
        picked.value = id
    }
}
