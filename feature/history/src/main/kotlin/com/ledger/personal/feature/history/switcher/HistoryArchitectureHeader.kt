package com.ledger.personal.feature.history.switcher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.components.LedgerOneCard
import com.ledger.personal.ui.components.LedgerOneText
import com.ledger.personal.ui.components.LedgerOneTextRole
import com.ledger.personal.ui.theme.colorScheme

/** Banner component highlighting the architecture currently running the screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryArchitectureHeader(
    architecture: HistoryArchitecture,
    modifier: Modifier = Modifier,
) {
    LedgerOneCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            LedgerOneText(
                text = "${architecture.displayName} · ${architecture.authorOrCompany}",
                role = LedgerOneTextRole.Title,
                color = colorScheme.primary,
            )
            LedgerOneText(
                text = architecture.paradigm,
                role = LedgerOneTextRole.Label,
                color = colorScheme.secondary,
            )
            LedgerOneText(
                text = architecture.summary,
                role = LedgerOneTextRole.Body,
                color = colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                architecture.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        LedgerOneText(
                            text = tag,
                            role = LedgerOneTextRole.Label,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
