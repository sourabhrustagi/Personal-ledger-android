package com.ledger.personal.testing

import com.ledger.personal.data.remote.dto.TransactionDto

fun sampleDto(
    id: String = "tx-1",
    amountInCents: Long = 2550L,
    currency: String = "USD",
    category: String = "Dining",
    description: String = "Lunch",
    timestampEpochMs: Long = 1_715_000_000_000L
) = TransactionDto(
    id = id,
    amountInCents = amountInCents,
    currency = currency,
    category = category,
    description = description,
    timestampEpochMs = timestampEpochMs
)
