package com.ledger.personal.testing

import com.ledger.personal.domain.model.Transaction

fun sampleTransaction(
    id: String = "tx-1",
    amountInCents: Long = 2550L,
    currency: String = "USD",
    category: String = "Dining",
    description: String = "Lunch",
    timestampEpochMs: Long = 1_715_000_000_000L
) = Transaction(
    id = id,
    amountInCents = amountInCents,
    currency = currency,
    category = category,
    description = description,
    timestampEpochMs = timestampEpochMs
)
