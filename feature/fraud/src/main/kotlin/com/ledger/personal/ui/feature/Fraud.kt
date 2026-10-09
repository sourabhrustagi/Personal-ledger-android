package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import com.ledger.personal.core.FraudBlocker
import com.ledger.personal.core.canReviewFraud
import com.ledger.personal.data.local.FraudBlockQueue
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.SeedFraudAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FraudUiState(
    val isLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val alerts: List<SeedFraudAlert> = MockCatalog.fraudAlerts,
    val pendingId: String? = null,
    val blockedIds: Set<String> = emptySet(),
    val clearedIds: Set<String> = emptySet(),
    val pending: Int = 0,
    val blocked: Int = 0,
    val errorMessage: String? = null,
) {
    val open: List<SeedFraudAlert> get() = alerts.filter { it.id !in blockedIds && it.id !in clearedIds }
}

class FraudViewModel : ViewModel() {
    private val blocker = FraudBlocker()
    private val state = MutableStateFlow(FraudUiState())
    val uiState: StateFlow<FraudUiState> = state.asStateFlow()

    fun schedule(id: String) {
        blocker.schedule(id)
        FraudBlockQueue.add(id)
        state.update { it.copy(pending = 1, pendingId = id, isEmpty = false, errorMessage = null) }
    }

    fun undo(id: String) {
        blocker.undo(id)
        FraudBlockQueue.undo(id)
        state.update { it.copy(pending = 0, pendingId = null) }
    }

    fun commit() {
        blocker.commit()
        state.update { it.copy(pending = 0, pendingId = null, blocked = blocker.blocked.size, blockedIds = blocker.blocked.toSet()) }
    }

    fun clear(id: String) = state.update { it.copy(clearedIds = it.clearedIds + id) }

    fun openAs(role: String) {
        if (!canReviewFraud(role)) {
            state.update { it.copy(errorMessage = "Analyst role required") }
        }
    }
}
