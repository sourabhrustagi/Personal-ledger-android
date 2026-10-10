package com.ledger.personal.feature.pos

import com.ledger.personal.core.PayEvent
import com.ledger.personal.core.PayState
import com.ledger.personal.core.next

/** Settle. */
fun settle(state: PayState, event: PayEvent) = state.next(event)
