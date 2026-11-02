package com.ledger.personal.feature.history.circuit

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Slack Circuit Screen marker contract.
 * Represents a destination or screen definition in Circuit.
 */
interface CircuitScreen

/**
 * Slack Circuit UI State contract.
 * In Circuit, every UI state embeds an eventSink callback lambda.
 */
interface CircuitUiState

/**
 * Slack Circuit Presenter contract.
 * Implements a composable [present] function that produces a [CircuitUiState].
 */
interface CircuitPresenter<UiState : CircuitUiState> {
    @Composable
    fun present(): UiState
}

/**
 * Slack Circuit UI contract.
 * Renders UI from a given [CircuitUiState].
 */
interface CircuitUi<UiState : CircuitUiState> {
    @Composable
    fun Content(state: UiState, modifier: Modifier)
}

