package com.ledger.personal.feature.wallet.payment

/**
 * Finite State Machine modeling the payment lifecycle to guarantee idempotency,
 * prevent double charging, and safely handle mid-transaction network drops.
 */
sealed interface PaymentFsmState {
    val recipient: String
    val amountMinor: Long
    val note: String
    val idempotencyKey: String

    /** Initial entry form state. */
    data class Form(
        override val recipient: String = "",
        override val amountMinor: Long = 0L,
        override val note: String = "",
        override val idempotencyKey: String = "transfer-1",
        val validationError: String? = null,
        val isInsufficientFunds: Boolean = false,
    ) : PaymentFsmState

    /** Parameters locked, awaiting user step-up confirmation. */
    data class AwaitingAuth(
        override val recipient: String,
        override val amountMinor: Long,
        override val note: String,
        override val idempotencyKey: String,
    ) : PaymentFsmState

    /** Transaction in-flight or retrying on transient network drops. */
    data class Submitting(
        override val recipient: String,
        override val amountMinor: Long,
        override val note: String,
        override val idempotencyKey: String,
        val attempt: Int = 1,
    ) : PaymentFsmState

    /**
     * Unconfirmed/In-doubt state when retries exhaust due to connection drop.
     * Prevents duplicate debit by mandating status inquiry / reconciliation before fresh submission.
     */
    data class InDoubt(
        override val recipient: String,
        override val amountMinor: Long,
        override val note: String,
        override val idempotencyKey: String,
        val message: String,
    ) : PaymentFsmState

    /** Terminal success state with confirmed debit. */
    data class Success(
        override val recipient: String,
        override val amountMinor: Long,
        override val note: String,
        override val idempotencyKey: String,
        val postedMinor: Long,
    ) : PaymentFsmState

    /** Terminal failure (business rejection or definitive failure). */
    data class Failed(
        override val recipient: String,
        override val amountMinor: Long,
        override val note: String,
        override val idempotencyKey: String,
        val reason: String,
        val canRetry: Boolean = false,
    ) : PaymentFsmState
}

/** Transitions driving the payment FSM. */
sealed interface PaymentFsmEvent {
    data class SetRecipient(val recipient: String) : PaymentFsmEvent
    data class SetAmount(val amountMinor: Long) : PaymentFsmEvent
    data class SetNote(val note: String) : PaymentFsmEvent
    data class Review(val balanceMinor: Long) : PaymentFsmEvent
    data object Edit : PaymentFsmEvent
    data class AuthSuccess(val authenticated: Boolean) : PaymentFsmEvent
    data class RetryAttempt(val attempt: Int) : PaymentFsmEvent
    data class SubmitSuccess(val postedMinor: Long) : PaymentFsmEvent
    data class SubmitTransientFailure(val message: String) : PaymentFsmEvent
    data class SubmitTerminalFailure(val reason: String) : PaymentFsmEvent
    data class Reconciled(val postedMinor: Long?) : PaymentFsmEvent
    data class Reset(val newIdempotencyKey: String) : PaymentFsmEvent
}

/**
 * Pure transition reducer for the Payment FSM.
 * Ensures strict state isolation and invalidates illegal transitions.
 */
object PaymentFsmReducer {
    fun reduce(current: PaymentFsmState, event: PaymentFsmEvent): PaymentFsmState = when (event) {
        is PaymentFsmEvent.SetRecipient -> when (current) {
            is PaymentFsmState.Form -> current.copy(recipient = event.recipient, validationError = null)
            else -> current
        }
        is PaymentFsmEvent.SetAmount -> when (current) {
            is PaymentFsmState.Form -> current.copy(
                amountMinor = event.amountMinor,
                validationError = null,
                isInsufficientFunds = false,
            )
            else -> current
        }
        is PaymentFsmEvent.SetNote -> when (current) {
            is PaymentFsmState.Form -> current.copy(note = event.note)
            else -> current
        }
        is PaymentFsmEvent.Review -> when (current) {
            is PaymentFsmState.Form -> when {
                current.amountMinor <= 0L -> current.copy(validationError = "Amount must be strictly positive")
                current.amountMinor > event.balanceMinor -> current.copy(
                    isInsufficientFunds = true,
                    validationError = "Insufficient funds",
                )
                current.recipient.isBlank() -> current.copy(validationError = "Recipient cannot be empty")
                else -> PaymentFsmState.AwaitingAuth(
                    recipient = current.recipient,
                    amountMinor = current.amountMinor,
                    note = current.note,
                    idempotencyKey = current.idempotencyKey,
                )
            }
            else -> current
        }
        PaymentFsmEvent.Edit -> when (current) {
            is PaymentFsmState.AwaitingAuth,
            is PaymentFsmState.Failed,
            is PaymentFsmState.InDoubt -> PaymentFsmState.Form(
                recipient = current.recipient,
                amountMinor = current.amountMinor,
                note = current.note,
                idempotencyKey = current.idempotencyKey,
            )
            else -> current
        }
        is PaymentFsmEvent.AuthSuccess -> when (current) {
            is PaymentFsmState.AwaitingAuth -> if (event.authenticated) {
                PaymentFsmState.Submitting(
                    recipient = current.recipient,
                    amountMinor = current.amountMinor,
                    note = current.note,
                    idempotencyKey = current.idempotencyKey,
                    attempt = 1,
                )
            } else current
            is PaymentFsmState.Failed -> if (event.authenticated && current.canRetry) {
                PaymentFsmState.Submitting(
                    recipient = current.recipient,
                    amountMinor = current.amountMinor,
                    note = current.note,
                    idempotencyKey = current.idempotencyKey,
                    attempt = 1,
                )
            } else current
            else -> current
        }
        is PaymentFsmEvent.RetryAttempt -> when (current) {
            is PaymentFsmState.Submitting -> current.copy(attempt = event.attempt)
            else -> current
        }
        is PaymentFsmEvent.SubmitSuccess -> when (current) {
            is PaymentFsmState.Submitting, is PaymentFsmState.InDoubt -> PaymentFsmState.Success(
                recipient = current.recipient,
                amountMinor = current.amountMinor,
                note = current.note,
                idempotencyKey = current.idempotencyKey,
                postedMinor = event.postedMinor,
            )
            else -> current
        }
        is PaymentFsmEvent.SubmitTransientFailure -> when (current) {
            is PaymentFsmState.Submitting -> PaymentFsmState.InDoubt(
                recipient = current.recipient,
                amountMinor = current.amountMinor,
                note = current.note,
                idempotencyKey = current.idempotencyKey,
                message = event.message,
            )
            else -> current
        }
        is PaymentFsmEvent.SubmitTerminalFailure -> when (current) {
            is PaymentFsmState.Submitting -> PaymentFsmState.Failed(
                recipient = current.recipient,
                amountMinor = current.amountMinor,
                note = current.note,
                idempotencyKey = current.idempotencyKey,
                reason = event.reason,
                canRetry = false,
            )
            else -> current
        }
        is PaymentFsmEvent.Reconciled -> when (current) {
            is PaymentFsmState.InDoubt -> if (event.postedMinor != null) {
                PaymentFsmState.Success(
                    recipient = current.recipient,
                    amountMinor = current.amountMinor,
                    note = current.note,
                    idempotencyKey = current.idempotencyKey,
                    postedMinor = event.postedMinor,
                )
            } else {
                PaymentFsmState.Failed(
                    recipient = current.recipient,
                    amountMinor = current.amountMinor,
                    note = current.note,
                    idempotencyKey = current.idempotencyKey,
                    reason = "Payment was not recorded by the bank. You may safely retry.",
                    canRetry = true,
                )
            }
            else -> current
        }
        is PaymentFsmEvent.Reset -> PaymentFsmState.Form(
            recipient = "",
            amountMinor = 0L,
            note = "",
            idempotencyKey = event.newIdempotencyKey,
        )
    }
}
