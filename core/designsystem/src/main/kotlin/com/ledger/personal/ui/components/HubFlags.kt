package com.ledger.personal.ui.components

/** Flags controlling hub visibility and mock conditions. */
data class HubFlags(
    val offline: Boolean = false,
    val empty: Boolean = false,
    val reveal: Boolean = false,
    val onToggleReveal: () -> Unit = {},
)
