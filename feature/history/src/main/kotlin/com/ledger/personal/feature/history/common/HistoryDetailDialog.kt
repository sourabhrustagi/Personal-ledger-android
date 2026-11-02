package com.ledger.personal.feature.history.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ledger.personal.core.toFormattedCurrency
import com.ledger.personal.core.toFormattedDateTime
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.ui.components.LedgerOneText
import com.ledger.personal.ui.components.LedgerOneTextRole

/** Dialog presenting details of a selected transaction. */
@Composable
fun HistoryDetailDialog(
    item: HistoryTransactionItem,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            LedgerOneText(
                text = item.description.ifBlank { item.category },
                role = LedgerOneTextRole.Title,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                LedgerOneText(
                    text = "Amount: ${item.amountInCents.toFormattedCurrency(item.currency)}",
                    role = LedgerOneTextRole.Body,
                )
                LedgerOneText(
                    text = "Category: ${item.category}",
                    role = LedgerOneTextRole.Label,
                )
                LedgerOneText(
                    text = "Date: ${item.timestampEpochMs.toFormattedDateTime()}",
                    role = LedgerOneTextRole.Label,
                )
                LedgerOneText(
                    text = "Transaction ID: ${item.id}",
                    role = LedgerOneTextRole.Label,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
    )
}
