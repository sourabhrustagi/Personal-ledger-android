package com.ledger.personal.data.local

object FraudBlockQueue {
    private val pending = linkedSetOf<String>()

    fun add(id: String) {
        pending += id
    }

    fun undo(id: String) {
        pending -= id
    }

    fun commit(id: String): Boolean = pending.remove(id)
}
