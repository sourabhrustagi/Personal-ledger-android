package com.ledger.personal.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PaymentsBackend
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
