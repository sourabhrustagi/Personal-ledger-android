package com.ledger.personal.domain.model

/**
 * Domain entity representing an immutable transaction.
 *
 * Money is stored in integer minor units (e.g. cents) to prevent floating-point inaccuracies.
 * Enforces business rule: amount must be strictly positive (> 0).
 */
data class Transaction(
    val id: String,
    val amountInCents: Long,
    val currency: String,
    val category: String,
    val description: String,
    val timestampEpochMs: Long
) {
    init {
        require(amountInCents > 0L) {
            "Amount must be strictly positive minor units, got: $amountInCents"
        }
        require(id.isNotBlank()) { "Transaction ID must not be blank" }
        require(currency.isNotBlank()) { "Currency code must not be blank" }
        require(category.isNotBlank()) { "Category must not be blank" }
    }
}
