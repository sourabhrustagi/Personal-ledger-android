package com.ledger.personal.data.remote.dto

import com.ledger.personal.domain.model.Transaction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Network payload for transaction. */
@Serializable
data class TransactionDto(
    @SerialName("id") val id: String,
    @SerialName("amount_in_cents") val amountInCents: Long,
    @SerialName("currency") val currency: String,
    @SerialName("category") val category: String,
    @SerialName("description") val description: String,
    @SerialName("timestamp_epoch_ms") val timestampEpochMs: Long
)

/** Network payload for transaction list response. */
@Serializable
data class TransactionListResponseDto(
    @SerialName("items") val items: List<TransactionDto>,
    @SerialName("page") val page: Int,
    @SerialName("page_size") val pageSize: Int,
    @SerialName("total_items") val totalItems: Long
)

/** Network payload for create transaction request. */
@Serializable
data class CreateTransactionRequestDto(
    @SerialName("amount_in_cents") val amountInCents: Long,
    @SerialName("currency") val currency: String,
    @SerialName("category") val category: String,
    @SerialName("description") val description: String
)

/** Network payload for patch transaction request. */
@Serializable
data class PatchTransactionRequestDto(
    @SerialName("category") val category: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("amount_in_cents") val amountInCents: Long? = null
)

/**
 * Mapper extension function translating network DTO to pure domain model.
 * Never expose network DTOs to UI or ViewModels.
 */
fun TransactionDto.toDomain(): Transaction {
    return Transaction(
        id = id,
        amountInCents = amountInCents,
        currency = currency,
        category = category,
        description = description,
        timestampEpochMs = timestampEpochMs
    )
}
