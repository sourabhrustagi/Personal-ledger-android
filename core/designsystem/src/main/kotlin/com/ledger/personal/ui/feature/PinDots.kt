package com.ledger.personal.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows pin dots. */
@Composable
fun PinDots(filled: Int, length: Int, modifier: Modifier = Modifier, error: Boolean = false) {
    val active = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier.semantics { contentDescription = "$filled of $length digits entered" },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(length) { index ->
            Box(
                Modifier.size(16.dp).clip(CircleShape)
                    .background(if (index < filled) active else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, active, CircleShape),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PinDotsPreview() {
    PersonalLedgerTheme {
        PinDots(filled = 2, length = 4)
    }
}
