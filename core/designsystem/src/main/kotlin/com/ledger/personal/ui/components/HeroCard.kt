package com.ledger.personal.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.components.LedgerOneText
import com.ledger.personal.ui.components.LedgerOneTextRole
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows hero card. */
@Composable
fun HeroCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val onHero = colors.onPrimary
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primary)
            .padding(20.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides onHero) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LedgerOneText(label, role = LedgerOneTextRole.Label, color = onHero)
                LedgerOneText(value, role = LedgerOneTextRole.Display, weight = FontWeight.Bold, color = onHero)
                supporting?.let { LedgerOneText(it, color = onHero) }
                footer()
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun HeroCardPreview() {
    PersonalLedgerTheme { HeroCard("Available", "₹12,400", supporting = "This month") }
}
