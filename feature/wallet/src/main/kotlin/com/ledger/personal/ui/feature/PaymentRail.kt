package com.ledger.personal.ui.feature

/** Sends one payment. [post] records it on the ledger and must be called at most once per accepted attempt. */
fun interface PaymentRail {
    /** Runs one attempt. */
    suspend operator fun invoke(post: () -> Long): Long
}
