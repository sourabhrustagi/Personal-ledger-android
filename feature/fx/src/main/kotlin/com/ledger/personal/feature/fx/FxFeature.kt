package com.ledger.personal.feature.fx

import com.ledger.personal.core.ThresholdWatch

/** Watch rate. */
fun watchRate(threshold: Long, ticks: Long) = ThresholdWatch(threshold).onRate(ticks)
