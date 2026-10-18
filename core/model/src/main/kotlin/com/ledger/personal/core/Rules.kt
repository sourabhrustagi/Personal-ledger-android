package com.ledger.personal.core

import kotlin.math.pow
import kotlin.math.roundToLong

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

/** Records a transfer once per idempotency key. */
class TransferBook {
    private val posted = LinkedHashMap<String, Long>()

    /** Stores the amount the first time this key is seen and returns that same amount afterwards. */
    fun submit(idempotencyKey: String, amountMinor: Long): Long = posted.getOrPut(idempotencyKey) { amountMinor }

    /** How many distinct transfers have been recorded. */
    val size: Int get() = posted.size

    /** Sum of every distinct transfer. */
    val total: Long get() = posted.values.sum()
}

/** Result of checking whether the actor may see a wallet. */
sealed interface WalletLookup {
    /** The actor is allowed to see this wallet. */
    data class Found(val owner: String) : WalletLookup
    /** The actor is not the owner, so the wallet is hidden. */
    data object NotFound : WalletLookup
}

/** Returns the wallet only when the actor is the owner. */
fun lookupWallet(actor: String, owner: String): WalletLookup =
    if (actor == owner) WalletLookup.Found(owner) else WalletLookup.NotFound

/** Fires once when a tick count crosses a threshold from below. */
class ThresholdWatch(private val threshold: Long) {
    private var above = false

    /** Returns true only on the tick that crosses the threshold. */
    fun onRate(ticks: Long): Boolean {
        val crossed = ticks >= threshold && !above
        above = ticks >= threshold
        return crossed
    }
}

/** A rate quote taken from a provider payload. */
data class ProviderQuote(val pair: String, val ticks: Long)

/** Reads the pair and the tick count out of a provider map. */
fun adaptQuote(fields: Map<String, String?>): ProviderQuote {
    val pair = requireNotNull(fields["pair"]) { "missing pair" }
    val rawTicks = requireNotNull(fields["ticks"]) { "missing ticks" }
    val ticks = requireNotNull(rawTicks.toLongOrNull()) { "ticks must be a number: $rawTicks" }
    return ProviderQuote(pair, ticks)
}

/** One captured KYC field and how confident the read was. */
data class CapturedField(val required: Boolean, val confidence: Double)

/** True when a required field was read below the confidence floor. */
fun needsHumanReview(fields: List<CapturedField>, floor: Double = 0.8): Boolean =
    fields.any { it.required && it.confidence < floor }

/** Result of comparing one field from two copies of a record. */
sealed interface FieldMerge {
    /** Both copies hold the same value. */
    data class Kept(val value: String) : FieldMerge
    /** The two copies disagree, so a person has to choose. */
    data class Conflict(val left: String, val right: String) : FieldMerge
}

/** Keeps the value when both sides match, otherwise reports a conflict. */
fun mergeField(left: String, right: String): FieldMerge =
    if (left == right) FieldMerge.Kept(left) else FieldMerge.Conflict(left, right)

/** Counts a charge id only the first time it is replayed. */
class ChargeQueue {
    private val posted = mutableSetOf<String>()
    var charges: Int = 0
        private set

    /** Increments the charge count for ids that have not been seen yet. */
    fun replay(ids: List<String>) {
        ids.forEach { id -> if (posted.add(id)) charges += 1 }
    }
}

/** Interest and principal portions of one EMI instalment, in minor units. */
data class EmiLeg(val interestMinor: Long, val principalMinor: Long)

/** Monthly instalment for a principal, an annual rate in basis points, and a term in months. */
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

/** Builds the instalment schedule. The last row clears the remaining balance. */
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

/** Runs a statement cycle only once. */
class StatementJob {
    private val done = mutableSetOf<String>()

    /** Returns true the first time this cycle id is run. */
    fun run(cycleId: String): Boolean = done.add(cycleId)
}

/** True when the digits are a plausible card number under the Luhn check. */
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

/** Replaces card numbers that pass the Luhn check with stars. */
fun redactPans(line: String): String = pan.replace(line) { match ->
    if (luhn(match.value)) "****" else match.value
}

/** True when the PIN is exactly four digits. */
fun mpinAccepted(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

/** Builds a UPI pay link for a VPA and an amount in paise. */
fun upiPayload(vpa: String, amountMinor: Long): String {
    val rupees = amountMinor / 100
    val paise = (amountMinor % 100).toString().padStart(2, '0')
    return "upi://pay?pa=$vpa&am=$rupees.$paise"
}

/** True while a new payee is still inside the 30 minute cooling period. */
fun coolingBlocks(addedAtEpochMs: Long, nowEpochMs: Long): Boolean = nowEpochMs - addedAtEpochMs < 30 * 60 * 1000L

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
