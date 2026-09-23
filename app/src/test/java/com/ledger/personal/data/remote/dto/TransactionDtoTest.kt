package com.ledger.personal.data.remote.dto

import com.ledger.personal.testing.sampleDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TransactionDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsDtoToDomain() {
        val domain = sampleDto(amountInCents = 9900L, category = "Rent").toDomain()
        assertEquals("tx-1", domain.id)
        assertEquals(9900L, domain.amountInCents)
        assertEquals("USD", domain.currency)
        assertEquals("Rent", domain.category)
        assertEquals("Lunch", domain.description)
        assertEquals(1_715_000_000_000L, domain.timestampEpochMs)
    }

    @Test
    fun serializesTransactionWithSnakeCaseKeys() {
        val encoded = json.encodeToString(TransactionDto.serializer(), sampleDto())
        assertEquals(true, encoded.contains("\"amount_in_cents\""))
        assertEquals(true, encoded.contains("\"timestamp_epoch_ms\""))
        val decoded = json.decodeFromString(TransactionDto.serializer(), encoded)
        assertEquals(sampleDto(), decoded)
    }

    @Test
    fun serializesListResponse() {
        val payload = TransactionListResponseDto(
            items = listOf(sampleDto()),
            page = 1,
            pageSize = 15,
            totalItems = 1
        )
        val encoded = json.encodeToString(TransactionListResponseDto.serializer(), payload)
        val decoded = json.decodeFromString(TransactionListResponseDto.serializer(), encoded)
        assertEquals(1, decoded.page)
        assertEquals(15, decoded.pageSize)
        assertEquals(1, decoded.totalItems)
        assertEquals("tx-1", decoded.items.single().id)
    }

    @Test
    fun patchRequestOmitsUnsetFieldsAsNull() {
        val encoded = json.encodeToString(
            PatchTransactionRequestDto.serializer(),
            PatchTransactionRequestDto(category = "Utilities")
        )
        val decoded = json.decodeFromString(PatchTransactionRequestDto.serializer(), encoded)
        assertEquals("Utilities", decoded.category)
        assertNull(decoded.description)
        assertNull(decoded.amountInCents)
    }
}
