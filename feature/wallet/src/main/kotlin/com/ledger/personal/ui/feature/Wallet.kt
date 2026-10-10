package com.ledger.personal.ui.feature

/** Allowed values of transfer step. */
enum class TransferStep { FORM, REVIEW, RECEIPT }

/** Allowed values of transfer status. */
enum class TransferStatus { IDLE, INSUFFICIENT, STEP_UP, PENDING, FAILED, DONE }

/** Values rendered by the wallet screen. */
data class WalletUiState(
    val isLoading: Boolean = false,
    val balanceMinor: Long = 0L,
    val amountMinor: Long = 0L,
    val recipient: String = "",
    val note: String = "",
    val step: TransferStep = TransferStep.FORM,
    val status: TransferStatus = TransferStatus.IDLE,
    val idempotencyKey: String = "transfer-1",
    val postedMinor: Long = 0L,
    val attempt: Int = 0,
    val errorMessage: String? = null,
)

/** Sends one payment. [post] records it on the ledger and must be called at most once per accepted attempt. */
fun interface PaymentRail {
    /** Runs one attempt. */
    suspend operator fun invoke(post: () -> Long): Long
}
