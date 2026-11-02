package com.ledger.personal.feature.history.model

/** Categories supported for filtering transaction history. */
enum class HistoryFilterCategory(val label: String) {
    ALL("All"),
    FOOD("Food"),
    SHOPPING("Shopping"),
    TRANSFER("Transfer"),
    BILLS("Bills"),
    SALARY("Salary"),
    ENTERTAINMENT("Entertainment");

    companion object {
        val ALL_CATEGORIES = entries.toList()
    }
}

