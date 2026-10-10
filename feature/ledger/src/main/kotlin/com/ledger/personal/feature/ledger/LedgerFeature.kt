package com.ledger.personal.feature.ledger

import com.ledger.personal.core.splitEvenly

/** Ledger page. */
fun ledgerPage(total: Long, parts: Int) = splitEvenly(total, parts)
