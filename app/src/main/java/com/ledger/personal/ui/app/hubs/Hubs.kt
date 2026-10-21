package com.ledger.personal.ui.app.hubs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.ui.app.HubFlags
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.PaymentsOfflineNotice
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.navigation.Screen
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows profile action. */
@Composable
internal fun ProfileAction(onProfile: () -> Unit) {
    IconButton(onClick = onProfile) { Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings") }
}

/** Shows hub link. */
@Composable
internal fun HubLink(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    ListRow(
        icon,
        title,
        subtitle = if (enabled) subtitle else "Unavailable offline",
        iconTint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        onClick = onClick.takeIf { enabled },
    )
}

/** Shows pay hub. */
@Composable
fun PayHub(
    flags: HubFlags,
    onProfile: () -> Unit,
    onOpen: (Screen) -> Unit,
) {
    FeatureScaffold("Pay", onBack = null, actions = { ProfileAction(onProfile) }) {
        if (flags.offline) PaymentsOfflineNotice()
        val online = !flags.offline
        SectionCard(title = "Send and receive money") {
            HubLink(Icons.AutoMirrored.Outlined.Send, "Send money", "To a contact or bank account", online) { onOpen(Screen.Wallet) }
            HubLink(Icons.Outlined.QrCode2, "Receive money", "Request via UPI or QR code", online) { onOpen(Screen.Banking) }
            HubLink(Icons.Outlined.QrCodeScanner, "Scan and pay", "Any UPI QR code", online) { onOpen(Screen.Scan) }
        }
    }
}

/** Shows the promotions stored on the device. */
@Composable
fun PromotionsHub(shelves: List<PromoTab>, selected: String, onSelect: (String) -> Unit, onProfile: () -> Unit) {
    FeatureScaffold("Offers", onBack = null, actions = { ProfileAction(onProfile) }) {
        if (shelves.isEmpty()) {
            Text("No promotions right now.")
        } else {
            val index = shelves.indexOfFirst { it.id == selected }.coerceAtLeast(0)
            PrimaryScrollableTabRow(selectedTabIndex = index, edgePadding = 16.dp) {
                shelves.forEachIndexed { i, shelf ->
                    Tab(selected = i == index, onClick = { onSelect(shelf.id) }, text = { Text(shelf.title) })
                }
            }
            val offers = shelves[index].offers
            if (offers.isEmpty()) {
                Text("Nothing in this tab.")
            } else {
                offers.forEach { offer ->
                    ListRow(Icons.Outlined.LocalOffer, offer.pitch, subtitle = offer.until.ifBlank { null })
                }
            }
        }
    }
}

private val previewFlags = HubFlags(offline = false, empty = false, reveal = true, onToggleReveal = {})

@PreviewLightDark
@Composable
private fun PayHubPreview() {
    PersonalLedgerTheme {
        PayHub(previewFlags, onProfile = {}, onOpen = {})
    }
}

@PreviewLightDark
@Composable
private fun PayHubOfflinePreview() {
    PersonalLedgerTheme {
        PayHub(
            HubFlags(offline = true, empty = false, reveal = true, onToggleReveal = {}),
            onProfile = {},
            onOpen = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PromotionsPreview() {
    PersonalLedgerTheme {
        PromotionsHub(
            shelves = listOf(PromoTab("bills", "Bills", listOf(PromoOffer("electricity", "₹25 cashback on electricity", "This week")))),
            selected = "bills",
            onSelect = {},
            onProfile = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PromotionsEmptyPreview() {
    PersonalLedgerTheme { PromotionsHub(shelves = emptyList(), selected = "", onSelect = {}, onProfile = {}) }
}
