package com.ledger.personal.feature.scan.model

import java.net.URLDecoder

/** Upi target. */
data class UpiTarget(val vpa: String, val name: String?, val amountMinor: Long)

private val vpaRegex = Regex("^[^\\s/@]+@[^\\s/@]+$")

/** Parses upi. */
fun parseUpi(raw: String): UpiTarget? {
    val text = raw.trim()
    if (text.startsWith("upi://pay?", ignoreCase = true)) {
        val params = text.substringAfter('?').split('&').mapNotNull { pair ->
            val key = pair.substringBefore('=', "")
            if (key.isEmpty()) null else key.lowercase() to URLDecoder.decode(pair.substringAfter("="), "UTF-8")
        }.toMap()
        val vpa = params["pa"]?.takeIf { it.contains('@') } ?: return null
        val amount = params["am"]?.let { com.ledger.personal.ui.components.parseMinor(it) } ?: 0L
        return UpiTarget(vpa, params["pn"], amount)
    }
    if (vpaRegex.matches(text)) {
        return UpiTarget(text, null, 0L)
    }
    return null
}
