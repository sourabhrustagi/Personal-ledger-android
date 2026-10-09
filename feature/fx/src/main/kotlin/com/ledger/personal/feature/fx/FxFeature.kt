package com.ledger.personal.feature.fx

import com.ledger.personal.core.ThresholdWatch

fun watchRate(threshold: Long, ticks: Long) = ThresholdWatch(threshold).onRate(ticks)
