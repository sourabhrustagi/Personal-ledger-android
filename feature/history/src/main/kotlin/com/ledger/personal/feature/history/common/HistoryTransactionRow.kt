package com.ledger.personal.feature.history.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ledger.personal.core.toFormattedCurrency
import com.ledger.personal.core.toFormattedDateTime
import com.ledger.personal.feature.history.model.HistoryTransactionItem
import com.ledger.personal.ui.components.ListRow
import com.ledger.personal.ui.components.categoryIcon
import com.ledger.personal.ui.theme.colorScheme

/** Single transaction item row adhering to design system. */
@Composable
fun HistoryTransactionRow(
    item: HistoryTransactionItem,
    onClick: (HistoryTransactionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedDate = remember(item.timestampEpochMs) { item.timestampEpochMs.toFormattedDateTime() }
    val formattedAmount = remember(item.amountInCents, item.currency) {
        item.amountInCents.toFormattedCurrency(item.currency)
    }

    ListRow(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(item) },
        icon = categoryIcon(item.category),
        title = item.description.ifBlank { item.category },
        subtitle = "${item.category} · $formattedDate",
        trailing = formattedAmount,
        trailingColor = colorScheme.onSurface,
    )
}

