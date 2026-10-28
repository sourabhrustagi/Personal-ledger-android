package com.ledger.personal.feature.hubs.offers

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.ListRow
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows the promotions stored on the device. */
@Composable
fun PromotionsHub(
    uiState: PromotionsUiState,
    onSelect: (String) -> Unit,
) {
    val shelves = uiState.shelves
    val selected = uiState.selected
    FeatureScaffold(stringResource(AppStrings.TITLE_OFFERS), onBack = null) {
        if (shelves.isEmpty()) {
            Text(stringResource(AppStrings.EMPTY_PROMOTIONS))
        } else {
            val index = shelves.indexOfFirst { it.id == selected }.coerceAtLeast(0)
            PrimaryScrollableTabRow(selectedTabIndex = index, edgePadding = 16.dp) {
                shelves.forEachIndexed { i, shelf ->
                    Tab(selected = i == index, onClick = { onSelect(shelf.id) }, text = { Text(shelf.title) })
                }
            }
            val offers = shelves[index].offers
            if (offers.isEmpty()) {
                Text(stringResource(AppStrings.EMPTY_TAB_CONTENT))
            } else {
                offers.forEach { offer ->
                    ListRow(Icons.Outlined.LocalOffer, offer.pitch, subtitle = offer.until.ifBlank { null })
                }
            }
        }
    }
}

/** Shows the promotions stored on the device. */
@Composable
fun PromotionsHub(
    shelves: List<PromoTab>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    PromotionsHub(PromotionsUiState(shelves, selected), onSelect)
}

@PreviewLightDark
@Composable
private fun PromotionsPreview() {
    PersonalLedgerTheme {
        PromotionsHub(
            shelves = listOf(PromoTab("bills", "Bills", listOf(PromoOffer("electricity", "₹25 cashback on electricity", "This week")))),
            selected = "bills",
            onSelect = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PromotionsEmptyPreview() {
    PersonalLedgerTheme { PromotionsHub(shelves = emptyList(), selected = "", onSelect = {}) }
}
