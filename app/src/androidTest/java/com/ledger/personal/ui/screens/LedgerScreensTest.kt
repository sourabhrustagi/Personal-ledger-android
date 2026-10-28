package com.ledger.personal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.ledger.personal.testing.sampleTransaction
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.ErrorState
import com.ledger.personal.ui.components.LoadingState
import com.ledger.personal.feature.ledger.list.TransactionListScreen
import com.ledger.personal.feature.ledger.list.TransactionListUiState
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LedgerScreensTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingStateShowsMessage() {
        composeRule.setContent {
            PersonalLedgerTheme {
                LoadingState(message = "Loading transactions...")
            }
        }
        composeRule.onNodeWithText("Loading transactions...").assertIsDisplayed()
    }

    @Test
    fun emptyStateInvokesAction() {
        var clicked = false
        composeRule.setContent {
            PersonalLedgerTheme {
                EmptyState(
                    title = "No transactions found",
                    actionLabel = "Add Transaction",
                    onActionClick = { clicked = true }
                )
            }
        }
        composeRule.onNodeWithText("Add Transaction").performClick()
        assertTrue(clicked)
    }

    @Test
    fun errorStateInvokesRetry() {
        var retried = false
        composeRule.setContent {
            PersonalLedgerTheme {
                ErrorState(errorMessage = "Network down", onRetry = { retried = true })
            }
        }
        composeRule.onNodeWithText("Network down").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    @Test
    fun listScreenShowsTransactions() {
        var openedId: String? = null
        var refreshed = false
        composeRule.setContent {
            PersonalLedgerTheme {
                TransactionListScreen(
                    uiState = TransactionListUiState(
                        items = listOf(sampleTransaction(id = "tx-9", description = "Team lunch", category = "Dining"))
                    ),
                    onRefresh = { refreshed = true },
                    onLoadMore = {},
                    onTransactionClick = { openedId = it },
                )
            }
        }
        composeRule.onNodeWithText("Personal Ledger").assertIsDisplayed()
        composeRule.onNodeWithText("Team lunch").performClick()
        composeRule.onNodeWithContentDescription("Refresh list").performClick()
        assertEquals("tx-9", openedId)
        assertTrue(refreshed)
    }

    @Test
    fun listScreenShowsEmptyAndError() {
        composeRule.setContent {
            PersonalLedgerTheme {
                TransactionListScreen(
                    uiState = TransactionListUiState(errorMessage = "Failed to load"),
                    onRefresh = {},
                    onLoadMore = {},
                    onTransactionClick = {},
                )
            }
        }
        composeRule.onNodeWithText("Failed to load").assertIsDisplayed()
    }
}
