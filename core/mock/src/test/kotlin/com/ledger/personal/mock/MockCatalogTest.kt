package com.ledger.personal.mock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockCatalogTest {

    @Test
    fun seedIsDeterministicAndCoversBothCurrencies() {
        assertEquals(200, MockCatalog.transactions.size)
        assertEquals(6, MockCatalog.accounts.size)
        assertEquals(setOf("INR", "USD"), MockCatalog.transactions.map { it.currency }.toSet())
        assertTrue(MockCatalog.transactions.all { it.amountMinor > 0L })
        assertEquals(MockCatalog.transactions, MockCatalog.transactions)
    }
}
