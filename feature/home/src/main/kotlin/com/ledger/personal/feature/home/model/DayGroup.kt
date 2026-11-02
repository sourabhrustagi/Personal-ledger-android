package com.ledger.personal.feature.home.model

import com.ledger.personal.domain.model.Transaction

/** Day group. */
data class DayGroup(val label: String, val items: List<Transaction>)

