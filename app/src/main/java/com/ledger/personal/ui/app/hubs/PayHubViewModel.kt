package com.ledger.personal.ui.app.hubs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PaymentsBackend
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val RECENT_TX_LIMIT = 5

/** Payees loaded from the payments team's endpoint and recent transactions. */
@HiltViewModel
class PayHubViewModel @Inject constructor(
    payments: PaymentsBackend,
    transactionRepository: TransactionRepository,
) : ViewModel() {
    private val state = MutableStateFlow<Result<List<PayShortcut>>?>(null)
    val shortcuts: StateFlow<Result<List<PayShortcut>>?> = state.asStateFlow()

    val recentTransactions: StateFlow<List<Transaction>> = transactionRepository.observeTransactions()
        .map { it.take(RECENT_TX_LIMIT) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { state.value = payments.shortcuts() }
    }
}
