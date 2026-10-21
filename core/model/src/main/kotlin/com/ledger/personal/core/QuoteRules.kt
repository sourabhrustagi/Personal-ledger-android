package com.ledger.personal.core

/** A rate quote taken from a provider payload. */
data class ProviderQuote(val pair: String, val ticks: Long)

/** Reads the pair and the tick count out of a provider map. */
fun adaptQuote(fields: Map<String, String?>): ProviderQuote {
    val pair = requireNotNull(fields["pair"]) { "missing pair" }
    val rawTicks = requireNotNull(fields["ticks"]) { "missing ticks" }
    val ticks = requireNotNull(rawTicks.toLongOrNull()) { "ticks must be a number: $rawTicks" }
    return ProviderQuote(pair, ticks)
}
