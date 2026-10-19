package com.ledger.personal.core

import java.util.concurrent.ConcurrentHashMap

/** Counts a charge id only the first time it is replayed. */
class ChargeQueue {
    private val posted = ConcurrentHashMap.newKeySet<String>()
    var charges: Int = 0
        private set

    /** Increments the charge count for ids that have not been seen yet. */
    @Synchronized
    fun replay(ids: List<String>) {
        ids.forEach { id -> if (posted.add(id)) charges += 1 }
    }
}

/** Amounts recorded against each spending category. */
class CategoryLedger {
    private val index = HashMap<String, MutableList<Long>>()

    /** Records an amount against a category. */
    fun add(category: String, amountMinor: Long) {
        index.getOrPut(category) { mutableListOf() } += amountMinor
    }

    /** Amounts already recorded for this category. */
    fun amounts(category: String): List<Long> = index[category].orEmpty()
}
