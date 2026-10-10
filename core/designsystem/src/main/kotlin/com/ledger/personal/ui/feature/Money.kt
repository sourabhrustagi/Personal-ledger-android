package com.ledger.personal.ui.feature

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/** Allowed values of money direction. */
enum class MoneyDirection { CREDIT, DEBIT, NEUTRAL }

/** Formats money. */
fun formatMoney(minor: Long, currency: String = "INR"): String {
    if (currency == "INR") return formatRupees(minor)
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    runCatching { format.currency = Currency.getInstance(currency) }
    return "${format.format(BigDecimal.valueOf(minor, 2))} $currency"
}

/** java.text.DecimalFormat only supports one grouping size, so lakh/crore grouping (1,29,900) is done by hand. */
private fun formatRupees(minor: Long): String {
    val abs = kotlin.math.abs(minor)
    val whole = (abs / 100).toString()
    val grouped = if (whole.length <= 3) {
        whole
    } else {
        whole.dropLast(3).reversed().chunked(2).joinToString(",").reversed() + "," + whole.takeLast(3)
    }
    val sign = if (minor < 0) "-" else ""
    return "$sign₹$grouped.${(abs % 100).toString().padStart(2, '0')}"
}

/** Formats signed. */
fun formatSigned(minor: Long, direction: MoneyDirection, currency: String = "INR"): String {
    val body = formatMoney(kotlin.math.abs(minor), currency)
    return when (direction) {
        MoneyDirection.CREDIT -> "+$body"
        MoneyDirection.DEBIT -> "\u2212$body"
        MoneyDirection.NEUTRAL -> body
    }
}

/** Spoken amount. */
fun spokenAmount(minor: Long, currency: String = "INR"): String {
    val whole = kotlin.math.abs(minor) / 100
    val fraction = kotlin.math.abs(minor) % 100
    val (major, sub) = when (currency) {
        "INR" -> "rupees" to "paise"
        "USD" -> "US dollars" to "cents"
        else -> currency to "hundredths"
    }
    return if (fraction == 0L) "$whole $major" else "$whole $major $fraction $sub"
}

/** Parses minor. */
fun parseMinor(text: String): Long? = runCatching {
    val value = BigDecimal(text.trim())
    if (value.signum() <= 0 || value.scale() > 2) null else value.movePointRight(2).longValueExact()
}.getOrNull()

/** Mask. */
fun mask(lastFour: String): String = "•• $lastFour"
