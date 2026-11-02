package com.ledger.personal.feature.history.switcher

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

/**
 * Architecture selector bar enabling the developer to switch dynamically
 * between all 8 state management frameworks.
 */
@Composable
fun HistoryArchitectureSwitcher(
    selectedArchitecture: HistoryArchitecture,
    onArchitectureSelected: (HistoryArchitecture) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HistoryArchitecture.ALL.forEach { arch ->
            val isSelected = arch == selectedArchitecture
            FilterChip(
                selected = isSelected,
                onClick = { onArchitectureSelected(arch) },
                label = { Text(text = arch.displayName) },
            )
        }
    }
}

