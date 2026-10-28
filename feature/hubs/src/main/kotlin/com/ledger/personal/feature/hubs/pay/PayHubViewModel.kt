package com.ledger.personal.feature.hubs.pay

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** UI state for PayHub. */
data class PayHubUiState(
    val recipientInput: String = "",
)

/**
 * Manages actions and state for the Pay hub screen.
 * Ensures all user click events are triggered directly on the ViewModel.
 */
@HiltViewModel
class PayHubViewModel @Inject constructor() : ViewModel() {

    private val mutableState = MutableStateFlow(PayHubUiState())

    /** Observable UI state for the PayHub screen. */
    val uiState: StateFlow<PayHubUiState> = mutableState.asStateFlow()

    /** Reacts to recipient text changes typed by the user. */
    fun onRecipientChanged(recipient: String) {
        mutableState.update { it.copy(recipientInput = recipient) }
    }

    /**
     * Triggered when the user clicks QR Scan row or Open Camera button.
     * Only invokes [onNavigateToScan] if connectivity is [online].
     */
    fun onScanClicked(online: Boolean, onNavigateToScan: () -> Unit) {
        if (online) {
            onNavigateToScan()
        }
    }

    /** Triggered when the user clicks contacts icon or Contacts button. */
    fun onPickContactClicked(launchPicker: () -> Unit) {
        launchPicker()
    }

    /** Triggered when a contact is selected from device contacts contract. */
    fun onContactPicked(phoneNumber: String) {
        mutableState.update { it.copy(recipientInput = phoneNumber) }
    }

    /** Triggered when the user clicks Continue to send to the entered recipient. */
    fun onContinueClicked(onNavigateToPay: (recipient: String) -> Unit) {
        val target = mutableState.value.recipientInput.trim()
        if (target.isNotBlank()) {
            onNavigateToPay(target)
        }
    }

    /** Triggered when the user clicks a saved contact from the quick contacts list. */
    fun onSavedContactClicked(handle: String, onNavigateToPay: (handle: String) -> Unit) {
        onNavigateToPay(handle)
    }
}
