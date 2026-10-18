package com.ledger.personal.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionTest {

    @Test
    fun rejectsZeroAmount() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            transaction(amountInCents = 0L)
        }
        assertTrue(exception.message.orEmpty().contains("Amount must be strictly positive"))
    }

    @Test
    fun rejectsNegativeAmount() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            transaction(amountInCents = -500L)
        }
        assertTrue(exception.message.orEmpty().contains("Amount must be strictly positive"))
    }

    @Test
    fun rejectsBlankId() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            transaction(id = "  ")
        }
        assertTrue(exception.message.orEmpty().contains("Transaction ID must not be blank"))
    }

    @Test
    fun rejectsBlankCurrency() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            transaction(currency = "")
        }
        assertTrue(exception.message.orEmpty().contains("Currency code must not be blank"))
    }

    @Test
    fun rejectsBlankCategory() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            transaction(category = "   ")
        }
        assertTrue(exception.message.orEmpty().contains("Category must not be blank"))
    }

    @Test
    fun acceptsValidPositiveAmount() {
        val tx = transaction(amountInCents = 2550L)
        assertEquals(2550L, tx.amountInCents)
        assertEquals("tx-3", tx.id)
        assertEquals("USD", tx.currency)
    }

    private fun transaction(
        id: String = "tx-3",
        amountInCents: Long = 2550L,
        currency: String = "USD",
        category: String = "Dining"
    ) = Transaction(
        id = id,
        amountInCents = amountInCents,
        currency = currency,
        category = category,
        description = "Lunch",
        timestampEpochMs = 1_715_000_000_000L
    )
}
