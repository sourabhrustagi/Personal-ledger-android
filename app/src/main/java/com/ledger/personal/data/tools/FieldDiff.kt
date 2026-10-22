package com.ledger.personal.data.tools

import com.ledger.personal.core.FieldMerge
import com.ledger.personal.core.mergeField
import com.ledger.personal.domain.model.Transaction

/** Field diff. */
data class FieldDiff(val field: String, val mine: String, val theirs: String)

/** Conflict fields. */
fun conflictFields(local: Transaction, server: Transaction): List<FieldDiff> = listOfNotNull(
    fieldDiff("Description", local.description, server.description),
    fieldDiff("Category", local.category, server.category),
    fieldDiff("Amount (minor units)", local.amountInCents.toString(), server.amountInCents.toString()),
)

private fun fieldDiff(field: String, mine: String, theirs: String): FieldDiff? =
    (mergeField(mine, theirs) as? FieldMerge.Conflict)?.let { FieldDiff(field, it.left, it.right) }

/** Builds the merged record from per-field choices; `true` keeps this device's value. */
fun resolveConflict(local: Transaction, server: Transaction, keepMine: Map<String, Boolean>): Transaction = server.copy(
    description = if (keepMine["Description"] == true) local.description else server.description,
    category = if (keepMine["Category"] == true) local.category else server.category,
    amountInCents = if (keepMine["Amount (minor units)"] == true) local.amountInCents else server.amountInCents,
)
