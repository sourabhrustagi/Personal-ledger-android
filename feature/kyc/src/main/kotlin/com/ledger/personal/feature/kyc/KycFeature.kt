package com.ledger.personal.feature.kyc

import com.ledger.personal.core.CapturedField

/** Review needed. */
fun reviewNeeded(confidence: Double) = com.ledger.personal.core.needsHumanReview(listOf(CapturedField(required = true, confidence = confidence)))
