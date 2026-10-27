package com.ledger.personal.ui.screens

import com.ledger.personal.testing.sampleTransaction
import com.ledger.personal.ui.screens.detail.TransactionDetailUiState
import com.ledger.personal.ui.screens.list.TransactionListUiState
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

    @Test
    fun detailIsEmptyWhenNotLoadingAndMissingTransaction() {
        assertTrue(TransactionDetailUiState().isEmpty)
        assertFalse(TransactionDetailUiState(isLoading = true).isEmpty)
        assertFalse(TransactionDetailUiState(errorMessage = "boom").isEmpty)
        assertFalse(TransactionDetailUiState(transaction = sampleTransaction()).isEmpty)
    }
}
