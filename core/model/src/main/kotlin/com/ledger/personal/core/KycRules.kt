package com.ledger.personal.core

/** One captured KYC field and how confident the read was. */
data class CapturedField(val required: Boolean, val confidence: Double)

/** True when a required field was read below the confidence floor. */
fun needsHumanReview(fields: List<CapturedField>, floor: Double = 0.8): Boolean =
    fields.any { it.required && it.confidence < floor }
