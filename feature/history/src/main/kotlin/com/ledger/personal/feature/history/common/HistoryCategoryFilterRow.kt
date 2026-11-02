package com.ledger.personal.feature.history.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ledger.personal.feature.history.model.HistoryFilterCategory

/** Horizontal filter chips row for filtering transactions by category. */
@Composable
fun HistoryCategoryFilterRow(
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HistoryFilterCategory.ALL_CATEGORIES.forEach { filter ->
            val isSelected = if (filter == HistoryFilterCategory.ALL) {
                selectedCategory == null || selectedCategory.equals("All", ignoreCase = true)
            } else {
                selectedCategory.equals(filter.label, ignoreCase = true)
            }
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (filter == HistoryFilterCategory.ALL) {
                        onCategorySelected(null)
                    } else {
                        onCategorySelected(filter.label)
                    }
                },
                label = { Text(text = filter.label) },
            )
        }
    }
}

