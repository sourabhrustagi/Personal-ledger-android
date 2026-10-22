package com.ledger.personal.domain.board

/** A person the payments team says you can pay. */
data class PayShortcut(val name: String, val handle: String)

/** Payments backend. Its JSON is not shared with the other teams. */
fun interface PaymentsBackend {
    /** Returns shortcuts. */
    suspend fun shortcuts(): Result<List<PayShortcut>>
}
