package com.ledger.personal.feature.kyc

import com.ledger.personal.core.CapturedField

fun reviewNeeded(confidence: Double) = com.ledger.personal.core.needsHumanReview(listOf(CapturedField(required = true, confidence = confidence)))
