package com.ledger.personal.ui.feature

import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.SeedFraudAlert

/** Values rendered by the fraud screen. */
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
