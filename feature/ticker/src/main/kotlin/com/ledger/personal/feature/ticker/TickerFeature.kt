package com.ledger.personal.feature.ticker

import com.ledger.personal.core.adaptQuote

/** Adapt tick. */
fun adaptTick(fields: Map<String, String?>) = adaptQuote(fields)
