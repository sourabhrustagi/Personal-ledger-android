package com.ledger.personal.feature.ledger.list

import com.ledger.personal.testing.sampleTransaction
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateTest {

    @Test
    fun listIsEmptyOnlyWhenIdleWithNoItemsOrError() {
        assertTrue(TransactionListUiState(isLoading = false).isEmpty)
        assertFalse(TransactionListUiState(isLoading = true).isEmpty)
        assertFalse(TransactionListUiState(errorMessage = "boom").isEmpty)
        assertFalse(TransactionListUiState(items = listOf(sampleTransaction())).isEmpty)
    }
}

