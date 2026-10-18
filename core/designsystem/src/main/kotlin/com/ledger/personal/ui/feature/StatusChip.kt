package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows status chip. */
@Composable
fun StatusChip(text: String, tone: BannerTone, modifier: Modifier = Modifier) {
    val color = tone.toneColor()
    Surface(modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@PreviewLightDark
@Composable
private fun StatusChipPreview() {
    PersonalLedgerTheme { StatusChip("Triggered", BannerTone.SUCCESS, Modifier.padding(16.dp)) }
}
