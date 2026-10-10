package com.ledger.personal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ledger.personal.domain.model.Money
import com.ledger.personal.domain.model.Transaction

/** Database row for transaction. */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val amount: Money,
    val currency: String,
    val category: String,
    val description: String,
    val timestampEpochMs: Long,
    val syncState: String,
    val updatedAtEpochMs: Long,
)

/** Converts this value to domain. */
fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    amountInCents = amount.minorUnits,
    currency = currency,
    category = category,
    description = description,
    timestampEpochMs = timestampEpochMs,
)

/** Converts this value to entity. */
fun Transaction.toEntity(syncState: String, updatedAtEpochMs: Long): TransactionEntity = TransactionEntity(
    id = id,
    amount = Money(amountInCents),
    currency = currency,
    category = category,
    description = description,
    timestampEpochMs = timestampEpochMs,
    syncState = syncState,
    updatedAtEpochMs = updatedAtEpochMs,
)
