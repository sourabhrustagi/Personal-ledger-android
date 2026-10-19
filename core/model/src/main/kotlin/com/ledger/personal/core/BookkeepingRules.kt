package com.ledger.personal.core

/** Splits a minor-unit total into equal parts. Earlier parts receive any leftover paise. */
fun splitEvenly(totalMinor: Long, parts: Int): List<Long> {
    require(totalMinor > 0L) { "Amount must be strictly positive" }
    require(parts > 0) { "Split needs at least one part" }
    val base = totalMinor / parts
    val extra = (totalMinor % parts).toInt()
    return List(parts) { index -> base + if (index < extra) 1L else 0L }
}

/** One side of a journal entry: an account with either a debit or a credit. */
data class Leg(val account: String, val debitMinor: Long, val creditMinor: Long)

/** Accepts the legs only when every row is a debit or a credit and the two sides balance. */
fun post(legs: List<Leg>) {
    require(legs.isNotEmpty())
    require(legs.all { it.debitMinor >= 0L && it.creditMinor >= 0L && (it.debitMinor == 0L || it.creditMinor == 0L) })
    val debits = legs.sumOf { it.debitMinor }
    val credits = legs.sumOf { it.creditMinor }
    require(debits == credits) { "Debits $debits differ from credits $credits" }
}
