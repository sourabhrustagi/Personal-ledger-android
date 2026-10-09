package com.ledger.personal.domain.model

/**
 * Integer minor units only. A Double amount is never stored.
 */
data class Money(val minorUnits: Long)
