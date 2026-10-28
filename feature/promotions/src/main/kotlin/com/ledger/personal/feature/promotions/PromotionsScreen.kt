package com.ledger.personal.feature.promotions

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.IconBadge
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows the promotions tab screen. */
@Composable
fun PromotionsScreen(
    uiState: PromotionsUiState,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    val shelves = uiState.shelves
    val selected = uiState.selected

    FeatureScaffold(
        title = stringResource(AppStrings.TITLE_OFFERS),
        onBack = null,
        scrollable = false,
    ) {
        if (shelves.isEmpty()) {
            Text(
                text = stringResource(AppStrings.EMPTY_PROMOTIONS),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        } else {
            val activeIndex = shelves.indexOfFirst { it.id == selected }.coerceAtLeast(0)
            PrimaryScrollableTabRow(selectedTabIndex = activeIndex, edgePadding = 16.dp) {
                shelves.forEachIndexed { index, shelf ->
                    Tab(
                        selected = index == activeIndex,
                        onClick = { onSelect(shelf.id) },
                        text = { Text(shelf.title) },
                    )
                }
            }
            val offers = shelves.getOrNull(activeIndex)?.offers.orEmpty()
            if (offers.isEmpty()) {
                Text(
                    text = stringResource(AppStrings.EMPTY_TAB_CONTENT),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(items = offers, key = { it.id }) { offer ->
                        PromotionCard(
                            offer = offer,
                            onClick = {
                                val query = Uri.encode(offer.pitch)
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                                try {
                                    context.startActivity(intent)
                                } catch (_: ActivityNotFoundException) {
                                    // Ignore if no browser installed
                                }
                            },
                        )
                    }
                }
            }
        }
    }

}

/** Individual promotion offer card. */
@Composable
private fun PromotionCard(
    offer: PromoOffer,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBadge(Icons.Outlined.LocalOffer, size = 40)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = offer.pitch,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (offer.until.isNotBlank()) {
                    Text(
                        text = offer.until,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PromotionsPreview() {
    PersonalLedgerTheme {
        PromotionsScreen(
            uiState = PromotionsUiState(
                shelves = listOf(
                    PromoTab(
                        "festive",
                        "Festive",
                        listOf(
                            PromoOffer("upi5", "5% back on UPI up to ₹100", "Until 31 Oct"),
                            PromoOffer("grocery", "₹50 off groceries above ₹500", "Until 20 Oct"),
                        ),
                    ),
                    PromoTab("bills", "Bills", emptyList()),
                ),
                selected = "festive",
            ),
            onSelect = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PromotionsEmptyPreview() {
    PersonalLedgerTheme {
        PromotionsScreen(
            uiState = PromotionsUiState(shelves = emptyList(), selected = ""),
            onSelect = {},
        )
    }
}
