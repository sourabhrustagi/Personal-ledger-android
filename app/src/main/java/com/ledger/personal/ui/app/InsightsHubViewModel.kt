package com.ledger.personal.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.board.InsightsBackend
import com.ledger.personal.domain.board.SpendHighlight
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Spending lines loaded from the insights team's endpoint. */
@HiltViewModel
class InsightsHubViewModel @Inject constructor(insights: InsightsBackend) : ViewModel() {
    private val state = MutableStateFlow<Result<List<SpendHighlight>>?>(null)
    val highlights: StateFlow<Result<List<SpendHighlight>>?> = state.asStateFlow()

    init {
        viewModelScope.launch { state.value = insights.highlights() }
    }
}
