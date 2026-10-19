package com.ledger.personal.core

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
