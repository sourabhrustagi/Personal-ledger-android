package com.ledger.personal.feature.banking

import com.ledger.personal.core.emiMinor

/** Installment. */
fun installment(principalMinor: Long, annualBps: Int, months: Int) = emiMinor(principalMinor, annualBps, months)
