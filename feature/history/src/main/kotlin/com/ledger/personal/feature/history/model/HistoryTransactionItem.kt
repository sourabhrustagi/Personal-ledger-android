package com.ledger.personal.feature.history.model

import androidx.compose.runtime.Immutable
import com.ledger.personal.domain.model.Transaction

/**
 * Immutable presentation model representing a transaction row in the History feature.
 * Money amount is strictly preserved as a 64-bit integer [Long] in minor units.
 */
@Immutable
data class HistoryTransactionItem(
    val id: String,
    val amountInCents: Long,
    val currency: String,
    val category: String,
    val description: String,
    val timestampEpochMs: Long,
) {
    val isCredit: Boolean get() = amountInCents >= 0
}

/** Maps a domain [Transaction] into presentation [HistoryTransactionItem]. */
fun Transaction.toItem(): HistoryTransactionItem = HistoryTransactionItem(
    id = id,
    amountInCents = amountInCents,
    currency = currency,
    category = category,
    description = description,
    timestampEpochMs = timestampEpochMs,
)

