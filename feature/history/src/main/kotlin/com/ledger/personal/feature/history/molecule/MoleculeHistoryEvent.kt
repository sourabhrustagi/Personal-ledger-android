package com.ledger.personal.feature.history.molecule

import com.ledger.personal.feature.history.model.HistoryTransactionItem

/** User events dispatched into the Molecule presenter. */
sealed interface MoleculeHistoryEvent {
    data class SelectCategory(val category: String?) : MoleculeHistoryEvent
    data class SelectTransaction(val transaction: HistoryTransactionItem?) : MoleculeHistoryEvent
    data object DismissDetail : MoleculeHistoryEvent
    data object Refresh : MoleculeHistoryEvent
}

