package com.ledger.personal.core

import kotlin.math.pow
import kotlin.math.roundToLong

fun splitEvenly(totalMinor: Long, parts: Int): List<Long> {
    require(totalMinor > 0L) { "Amount must be strictly positive" }
    require(parts > 0) { "Split needs at least one part" }
    val base = totalMinor / parts
    val extra = (totalMinor % parts).toInt()
    return List(parts) { index -> base + if (index < extra) 1L else 0L }
}

data class Leg(val account: String, val debitMinor: Long, val creditMinor: Long)

fun post(legs: List<Leg>) {
    require(legs.isNotEmpty())
    require(legs.all { it.debitMinor >= 0L && it.creditMinor >= 0L && (it.debitMinor == 0L || it.creditMinor == 0L) })
    val debits = legs.sumOf { it.debitMinor }
    val credits = legs.sumOf { it.creditMinor }
    require(debits == credits) { "Debits $debits differ from credits $credits" }
}

class TransferBook {
    private val posted = LinkedHashMap<String, Long>()

    fun submit(idempotencyKey: String, amountMinor: Long): Long = posted.getOrPut(idempotencyKey) { amountMinor }

    fun size(): Int = posted.size
}

sealed interface WalletLookup {
    data class Found(val owner: String) : WalletLookup
    data object NotFound : WalletLookup
}

fun lookupWallet(actor: String, owner: String): WalletLookup =
    if (actor == owner) WalletLookup.Found(owner) else WalletLookup.NotFound

class ThresholdWatch(private val threshold: Long) {
    private var above = false

    fun onRate(ticks: Long): Boolean {
        val crossed = ticks >= threshold && !above
        above = ticks >= threshold
        return crossed
    }
}

data class ProviderQuote(val pair: String, val ticks: Long)

fun adaptQuote(fields: Map<String, String?>): ProviderQuote {
    val pair = fields["pair"] ?: throw IllegalArgumentException("missing pair")
    val ticks = fields["ticks"] ?: throw IllegalArgumentException("missing ticks")
    return ProviderQuote(pair, ticks.toLong())
}

data class CapturedField(val required: Boolean, val confidence: Double)

fun needsHumanReview(fields: List<CapturedField>, floor: Double = 0.8): Boolean =
    fields.any { it.required && it.confidence < floor }

sealed interface FieldMerge {
    data class Kept(val value: String) : FieldMerge
    data class Conflict(val left: String, val right: String) : FieldMerge
}

fun mergeField(left: String, right: String): FieldMerge =
    if (left == right) FieldMerge.Kept(left) else FieldMerge.Conflict(left, right)

class ChargeQueue {
    private val posted = mutableSetOf<String>()
    var charges: Int = 0
        private set

    fun replay(ids: List<String>) {
        ids.forEach { id -> if (posted.add(id)) charges += 1 }
    }
}

data class EmiLeg(val interestMinor: Long, val principalMinor: Long)

fun emiMinor(principalMinor: Long, annualBps: Int, months: Int): Long {
    val monthly = annualBps.toDouble() / 12.0 / 10_000.0
    val growth = (1.0 + monthly).pow(months)
    // ponytail: closed-form annuity uses Double, then rounds to minor units
    return (principalMinor * monthly * growth / (growth - 1.0)).roundToLong()
}

/** Months needed to clear [principalMinor] at a fixed [emiMinor]; used when a prepayment keeps the EMI and shortens the loan. */
fun tenureFor(principalMinor: Long, annualBps: Int, emiMinor: Long): Int {
    if (principalMinor <= 0L) return 0
    val monthly = annualBps.toDouble() / 12.0 / 10_000.0
    val ratio = monthly * principalMinor / emiMinor
    require(ratio < 1.0) { "EMI does not cover interest" }
    return kotlin.math.ceil(-kotlin.math.ln(1.0 - ratio) / kotlin.math.ln(1.0 + monthly) - 1e-3).toInt()
}

fun amortize(principalMinor: Long, annualBps: Int, months: Int): List<EmiLeg> {
    val emi = emiMinor(principalMinor, annualBps, months)
    val monthlyBps = annualBps / 12
    var balance = principalMinor
    val legs = ArrayList<EmiLeg>(months)
    repeat(months - 1) {
        val interest = balance * monthlyBps / 10_000
        val principalPart = (emi - interest).coerceAtMost(balance)
        legs += EmiLeg(interest, principalPart)
        balance -= principalPart
    }
    legs += EmiLeg(balance * monthlyBps / 10_000, balance)
    return legs
}

class StatementJob {
    private val done = mutableSetOf<String>()

    fun run(cycleId: String): Boolean = done.add(cycleId)
}

class FraudBlocker {
    private val pending = linkedSetOf<String>()
    val blocked = mutableListOf<String>()

    fun schedule(id: String) {
        pending += id
    }

    fun undo(id: String) {
        pending -= id
    }

    fun commit() {
        blocked += pending
        pending.clear()
    }
}

fun luhn(number: String): Boolean {
    val digits = number.filter { it.isDigit() }
    if (digits.length !in 13..19) return false
    var sum = 0
    var doubleIt = false
    for (index in digits.lastIndex downTo 0) {
        var n = digits[index].digitToInt()
        if (doubleIt) {
            n *= 2
            if (n > 9) n -= 9
        }
        sum += n
        doubleIt = !doubleIt
    }
    return sum % 10 == 0
}

private val pan = Regex("(?<!\\d)(?:\\d[ -]?){13,19}")

fun redactPans(line: String): String = pan.replace(line) { match ->
    if (luhn(match.value)) "****" else match.value
}

enum class PayState { IDLE, READ, AUTHORIZED, SETTLED }

enum class PayEvent { TAP, APPROVE, SETTLE }

fun PayState.next(event: PayEvent): PayState = when (this to event) {
    PayState.IDLE to PayEvent.TAP -> PayState.READ
    PayState.READ to PayEvent.APPROVE -> PayState.AUTHORIZED
    PayState.AUTHORIZED to PayEvent.SETTLE -> PayState.SETTLED
    else -> error("Illegal transition $this -> $event")
}

fun canReviewFraud(role: String): Boolean = role == "ANALYST"

fun mpinAccepted(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

fun upiPayload(vpa: String, amountMinor: Long): String {
    val rupees = amountMinor / 100
    val paise = (amountMinor % 100).toString().padStart(2, '0')
    return "upi://pay?pa=$vpa&am=$rupees.$paise"
}

fun coolingBlocks(addedAtEpochMs: Long, nowEpochMs: Long): Boolean = nowEpochMs - addedAtEpochMs < 30 * 60 * 1000L

class CategoryLedger {
    private val index = HashMap<String, MutableList<Long>>()

    fun add(category: String, amountMinor: Long) {
        index.getOrPut(category) { mutableListOf() } += amountMinor
    }

    fun amounts(category: String): List<Long> = index[category].orEmpty()
}
