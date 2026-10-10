package com.ledger.personal.feature.assistant

import com.ledger.personal.core.ChargeQueue

/** Replay turns. */
fun replayTurns(ids: List<String>): Int {
    val queue = ChargeQueue()
    queue.replay(ids)
    return queue.charges
}
