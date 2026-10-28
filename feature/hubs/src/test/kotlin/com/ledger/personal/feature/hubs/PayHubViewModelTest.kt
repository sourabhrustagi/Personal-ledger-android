package com.ledger.personal.feature.hubs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PayHubViewModelTest {

    @Test
    fun initialRecipientIsEmpty() {
        val viewModel = PayHubViewModel()
        assertEquals("", viewModel.uiState.value.recipientInput)
    }

    @Test
    fun onRecipientChangedUpdatesState() {
        val viewModel = PayHubViewModel()
        viewModel.onRecipientChanged("alice@upi")
        assertEquals("alice@upi", viewModel.uiState.value.recipientInput)
    }

    @Test
    fun onScanClickedNavigatesOnlyWhenOnline() {
        val viewModel = PayHubViewModel()
        var scanned = false

        // Offline: should not trigger
        viewModel.onScanClicked(online = false) { scanned = true }
        assertFalse(scanned)

        // Online: should trigger
        viewModel.onScanClicked(online = true) { scanned = true }
        assertTrue(scanned)
    }

    @Test
    fun onPickContactClickedLaunchesPicker() {
        val viewModel = PayHubViewModel()
        var launched = false
        viewModel.onPickContactClicked { launched = true }
        assertTrue(launched)
    }

    @Test
    fun onContactPickedUpdatesRecipientInput() {
        val viewModel = PayHubViewModel()
        viewModel.onContactPicked("+919876543210")
        assertEquals("+919876543210", viewModel.uiState.value.recipientInput)
    }

    @Test
    fun onContinueClickedNavigatesWhenRecipientIsNotBlank() {
        val viewModel = PayHubViewModel()
        var targetRecipient: String? = null

        // Blank: should not navigate
        viewModel.onContinueClicked { targetRecipient = it }
        assertEquals(null, targetRecipient)

        // Non-blank: should navigate with trimmed recipient
        viewModel.onRecipientChanged("  bob@okhdfcbank  ")
        viewModel.onContinueClicked { targetRecipient = it }
        assertEquals("bob@okhdfcbank", targetRecipient)
    }

    @Test
    fun onSavedContactClickedNavigatesWithHandle() {
        val viewModel = PayHubViewModel()
        var targetHandle: String? = null
        viewModel.onSavedContactClicked("carol@upi") { targetHandle = it }
        assertEquals("carol@upi", targetHandle)
    }
}
