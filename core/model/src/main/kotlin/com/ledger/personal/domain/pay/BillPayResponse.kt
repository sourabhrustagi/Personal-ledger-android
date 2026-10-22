package com.ledger.personal.domain.pay

/**
 * Domain response model returned upon successful bill payment submission.
 */
data class BillPayResponse(
    val status: String = "SUCCESS",
    val message: String = "Bill paid successfully",
)
