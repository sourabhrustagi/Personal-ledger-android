package com.ledger.personal.core

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.math.roundToLong

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
    private val done = ConcurrentHashMap.newKeySet<String>()

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
