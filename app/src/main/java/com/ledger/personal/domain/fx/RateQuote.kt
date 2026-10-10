package com.ledger.personal.domain.fx

/** Rate quote. */
data class RateQuote(val pair: String, val ticks: Long)

/** Contract for rate disposition. */
sealed interface RateDisposition {
    /** Served. */
    data class Served(val quote: RateQuote) : RateDisposition
    /** Quarantined. */
    data class Quarantined(val quote: RateQuote) : RateDisposition
}

private const val SIGMA_LIMIT = 10L

/**
 * Sample standard deviation, compared in integers so a rate tick never becomes a Double.
 * |z| >= 10 is quarantined: kept, and not served.
 */
fun disposeQuote(historyTicks: List<Long>, quote: RateQuote): RateDisposition {
    require(historyTicks.size >= 2) { "Need at least two prior ticks to measure sigma" }
    val n = historyTicks.size.toLong()
    val sum = historyTicks.sum()
    val sumSquares = historyTicks.sumOf { diff ->
        val scaled = n * diff - sum
        scaled * scaled
    }
    val candidate = n * quote.ticks - sum
    val beyondTenSigma = if (sumSquares == 0L) {
        candidate != 0L
    } else {
        candidate * candidate * (n - 1) >= SIGMA_LIMIT * SIGMA_LIMIT * sumSquares
    }
    return if (beyondTenSigma) {
        RateDisposition.Quarantined(quote)
    } else {
        RateDisposition.Served(quote)
    }
}
