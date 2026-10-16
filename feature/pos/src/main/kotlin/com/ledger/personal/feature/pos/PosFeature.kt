package com.ledger.personal.feature.pos

/** Receipt key written when a bill is paid. */
fun billReceipt(biller: String, amountMinor: Long) = "$biller:$amountMinor"
