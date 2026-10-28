package com.ledger.personal.feature.wallet

import com.ledger.personal.feature.wallet.ui.TransferStatus
import com.ledger.personal.feature.wallet.ui.TransferStep
import com.ledger.personal.feature.wallet.ui.WalletViewModel
import com.ledger.personal.mock.MockCatalog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WalletViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun walletRejectsInsufficientFundsAndRetriesOnce() {
        val viewModel = WalletViewModel(rail = { post -> post() }, retryDelayMs = 0)
        viewModel.onAmount(viewModel.uiState.value.balanceMinor + 1)
        viewModel.review()
        assertEquals(TransferStatus.INSUFFICIENT, viewModel.uiState.value.status)

        viewModel.onAmount(100L)
        viewModel.review()
        viewModel.onStepUp(true)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.retry()
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransferStep.RECEIPT, viewModel.uiState.value.step)
        assertEquals(1, viewModel.postedCount)
    }

    @Test
    fun droppedResponseAfterPostingIsRetriedWithoutChargingTwice() {
        var calls = 0
        val viewModel = WalletViewModel(
            rail = { post ->
                calls++
                val posted = post()
                if (calls == 1) throw java.io.IOException("dropped after posting")
                posted
            },
            retryDelayMs = 0,
        )
        val opening = viewModel.uiState.value.balanceMinor
        viewModel.onAmount(100L)
        viewModel.review()
        viewModel.onStepUp(true)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransferStatus.DONE, viewModel.uiState.value.status)
        assertEquals(2, calls)
        assertEquals(1, viewModel.postedCount)
        assertEquals(opening - 100L, viewModel.uiState.value.balanceMinor)
    }

    @Test
    fun declinedPaymentShowsAMessageAndIsNotRetried() {
        var calls = 0
        val viewModel = WalletViewModel(
            rail = {
                calls++
                throw com.ledger.personal.core.TransactionRejected("Your bank declined this payment")
            },
            retryDelayMs = 0,
        )
        viewModel.onAmount(100L)
        viewModel.review()
        viewModel.onStepUp(true)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(TransferStatus.FAILED, viewModel.uiState.value.status)
        assertEquals(1, calls)
        assertEquals(0, viewModel.postedCount)
        assertTrue(viewModel.uiState.value.errorMessage.orEmpty().contains("Nothing was changed on the server"))
    }
}
