package com.ledger.personal.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.ErrorState
import com.ledger.personal.ui.components.LoadingState
import com.ledger.personal.ui.theme.PersonalLedgerTheme
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
}
