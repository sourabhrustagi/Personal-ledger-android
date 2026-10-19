package com.ledger.personal.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Timeline step. */
data class TimelineStep(val label: String, val detail: String?, val done: Boolean)

/** Shows timeline. */
@Composable
fun Timeline(steps: List<TimelineStep>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEach { step ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.padding(top = 4.dp).size(12.dp).clip(CircleShape).background(
                        if (step.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                )
                Column {
                    Text(step.label, fontWeight = if (step.done) FontWeight.SemiBold else FontWeight.Normal)
                    step.detail?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TimelinePreview() {
    PersonalLedgerTheme {
        Timeline(
            listOf(
                TimelineStep("Submitted", "We've logged your dispute", true),
                TimelineStep("Under review", null, false),
            ),
        )
    }
}
