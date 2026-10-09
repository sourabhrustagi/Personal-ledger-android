package com.ledger.personal.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/**
 * Converts integer minor units (cents) to a localized currency string.
 * Example: 1250L -> "$12.50"
 */
fun Long.toFormattedCurrency(currencyCode: String = "USD"): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    runCatching {
        format.currency = Currency.getInstance(currencyCode)
    }
    val majorUnits = this.toDouble() / 100.0
    return format.format(majorUnits)
}

/**
 * Converts integer minor units (cents) to a decimal string for forms.
 * Example: 1250L -> "12.50"
 */
fun Long.toMajorUnitsString(): String {
    val whole = this / 100
    val remainder = Math.abs(this % 100)
    return String.format(Locale.US, "%d.%02d", whole, remainder)
}

/**
 * Parses user decimal input (e.g. "12.50" or "10") into integer minor units (e.g. 1250L).
 * Returns null if invalid or <= 0.
 */
fun String.toMinorUnits(): Long? {
    val clean = this.trim().replace(",", ".")
    if (clean.isBlank()) return null
    return runCatching {
        val parts = clean.split(".")
        val dollars = parts[0].toLongOrNull() ?: return null
        if (dollars < 0) return null

        val cents = when {
            parts.size == 1 -> 0L
            parts.size == 2 -> {
                val fraction = parts[1]
                when {
                    fraction.isEmpty() -> 0L
                    fraction.length == 1 -> (fraction + "0").toLong()
                    fraction.length == 2 -> fraction.toLong()
                    else -> fraction.substring(0, 2).toLong()
                }
            }
            else -> return null
        }
        val total = (dollars * 100L) + cents
        if (total > 0L) total else null
    }.getOrNull()
}

/**
 * Formats epoch timestamp to human-readable date.
 */
fun Long.toFormattedDateTime(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(this))
}
