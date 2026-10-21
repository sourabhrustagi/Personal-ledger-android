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
import com.ledger.personal.ui.screens.create.CreateTransactionScreen
import com.ledger.personal.ui.screens.create.CreateTransactionUiState
import com.ledger.personal.ui.screens.detail.TransactionDetailScreen
import com.ledger.personal.ui.screens.detail.TransactionDetailUiState
import com.ledger.personal.ui.screens.edit.EditTransactionScreen
import com.ledger.personal.ui.screens.edit.EditTransactionUiState
import com.ledger.personal.ui.screens.list.TransactionListScreen
import com.ledger.personal.ui.screens.list.TransactionListUiState
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
    fun listScreenShowsTransactionsAndCreateAction() {
        var createClicked = false
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
                    onCreateClick = { createClicked = true }
                )
            }
        }
        composeRule.onNodeWithText("Personal Ledger").assertIsDisplayed()
        composeRule.onNodeWithText("Team lunch").performClick()
        composeRule.onNodeWithContentDescription("New Transaction").performClick()
        composeRule.onNodeWithContentDescription("Refresh list").performClick()
        assertEquals("tx-9", openedId)
        assertTrue(createClicked)
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
                    onCreateClick = {}
                )
            }
        }
        composeRule.onNodeWithText("Failed to load").assertIsDisplayed()
    }

    @Test
    fun createScreenSubmitsTypedValues() {
        var amount: String? = null
        var category: String? = null
        var submitted = false
        composeRule.setContent {
            PersonalLedgerTheme {
                CreateTransactionScreen(
                    uiState = CreateTransactionUiState(),
                    onAmountChange = { amount = it },
                    onCategoryChange = { category = it },
                    onDescriptionChange = {},
                    onSubmit = { submitted = true },
                    onNavigateBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Amount (e.g. 25.50)").performTextInput("12.50")
        composeRule.onNodeWithText("Groceries").performClick()
        composeRule.onNodeWithText("Save Transaction").performClick()
        assertEquals("12.50", amount)
        assertEquals("Groceries", category)
        assertTrue(submitted)
    }

    @Test
    fun createScreenDisablesSubmitWhileRecording() {
        composeRule.setContent {
            PersonalLedgerTheme {
                CreateTransactionScreen(
                    uiState = CreateTransactionUiState(isSubmitting = true),
                    onAmountChange = {},
                    onCategoryChange = {},
                    onDescriptionChange = {},
                    onSubmit = {},
                    onNavigateBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Recording...").assertIsDisplayed()
        composeRule.onNodeWithText("Recording...").assertIsNotEnabled()
    }

    @Test
    fun detailScreenShowsTransactionDetails() {
        var editId: String? = null
        composeRule.setContent {
            PersonalLedgerTheme {
                TransactionDetailScreen(
                    uiState = TransactionDetailUiState(
                        transaction = sampleTransaction(id = "tx-9", description = "Team lunch"),
                    ),
                    onNavigateBack = {},
                    onNavigateEdit = { editId = it },
                    onRetry = {},
                )
            }
        }
        composeRule.onNodeWithText("Team lunch").assertIsDisplayed()
        composeRule.onNodeWithText("Transaction ID").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Edit Transaction").performClick()
        assertEquals("tx-9", editId)
    }

    @Test
    fun editScreenShowsLoadedValuesAndSubmits() {
        var submitted = false
        composeRule.setContent {
            PersonalLedgerTheme {
                EditTransactionScreen(
                    uiState = EditTransactionUiState(
                        isLoadingInitial = false,
                        transactionId = "tx-9",
                        amountInput = "25.50",
                        category = "Dining",
                        description = "Lunch"
                    ),
                    onAmountChange = {},
                    onCategoryChange = {},
                    onDescriptionChange = {},
                    onSubmit = { submitted = true },
                    onNavigateBack = {},
                    onRetry = {}
                )
            }
        }
        composeRule.onNodeWithText("Update Transaction").assertIsEnabled().performClick()
        assertTrue(submitted)
    }
}
