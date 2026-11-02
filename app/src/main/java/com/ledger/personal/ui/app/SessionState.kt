package com.ledger.personal.ui.app

/** Session state. */
data class SessionState(
    val unlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedUntilMs: Long = 0L,
    val revealBalances: Boolean = false,
    val signInMessage: String? = null,
)

