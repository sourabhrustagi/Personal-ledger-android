package com.ledger.personal.data.local

/** Shared fraud block queue. */
object FraudBlockQueue {
    private val pending = linkedSetOf<String>()

    /** Add. */
    fun add(id: String) {
        pending += id
    }

    /** Undo. */
    fun undo(id: String) {
        pending -= id
    }

    /** Commit. */
    fun commit(id: String): Boolean = pending.remove(id)
}
