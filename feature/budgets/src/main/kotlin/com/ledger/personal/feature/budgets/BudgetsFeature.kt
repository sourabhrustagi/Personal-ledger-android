package com.ledger.personal.feature.budgets

import com.ledger.personal.core.CategoryLedger

fun categorySpend(category: String, amounts: List<Long>): Long {
    val ledger = CategoryLedger()
    amounts.forEach { ledger.add(category, it) }
    return ledger.amounts(category).sum()
}
