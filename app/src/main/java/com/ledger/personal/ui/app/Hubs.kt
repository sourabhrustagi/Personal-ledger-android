package com.ledger.personal.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.CurrencyExchange
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.SpendHighlight
import com.ledger.personal.mock.SeedAccount
import com.ledger.personal.ui.feature.EmptyState
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.AmountText
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.MoneyDirection
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.MaskedAmount
import com.ledger.personal.ui.feature.PaymentsOfflineNotice
import com.ledger.personal.ui.feature.ProtectedBadge
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.mask
import com.ledger.personal.ui.navigation.Screen
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Shows profile action. */
@Composable
internal fun ProfileAction(onProfile: () -> Unit) {
    IconButton(onClick = onProfile) { Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings") }
}

/** Shows hub link. */
@Composable
private fun HubLink(
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

/** Shows money hub. */
@Composable
fun MoneyHub(flags: HubFlags, onProfile: () -> Unit, onOpen: (Screen) -> Unit) {
    val accounts = if (flags.empty) emptyList() else com.ledger.personal.mock.MockCatalog.bankAccounts
    val asOf = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
    FeatureScaffold("Money", onBack = null, subtitle = "Balances as of $asOf", actions = {
        ProtectedBadge(Modifier.padding(end = 4.dp))
        ProfileAction(onProfile)
    }) {
        if (accounts.isEmpty()) {
            EmptyState(Icons.Outlined.AccountBalance, "No accounts linked", "Link a bank account to see balances here.")
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(accounts) { AccountTile(it, flags) }
            }
        }
        SectionCard(title = "Accounts and products") {
            HubLink(Icons.Outlined.AccountBalance, "Accounts", "Statements, UPI and bills") { onOpen(Screen.Banking) }
            HubLink(Icons.Outlined.AccountBalanceWallet, "Wallet", "Balance and transfers") { onOpen(Screen.Wallet) }
            HubLink(Icons.Outlined.CreditCard, "Cards", "Freeze, limits and controls") { onOpen(Screen.Cards) }
            HubLink(Icons.Outlined.RequestQuote, "Loans", "EMI calculator and prepay") { onOpen(Screen.Loans) }
            HubLink(Icons.Outlined.Savings, "Deposits", "Fixed deposits at 7.25% p.a.") { onOpen(Screen.Deposits) }
            HubLink(Icons.Outlined.History, "All transactions", "Full paged history") { onOpen(Screen.LedgerHistory) }
            HubLink(Icons.Outlined.Description, "Statements", "Monthly PDF for Savings") { onOpen(Screen.Statements) }
        }
    }
}

/** Shows account tile. */
@Composable
private fun AccountTile(account: SeedAccount, flags: HubFlags) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primary) {
        androidx.compose.foundation.layout.Column(Modifier.padding(16.dp)) {
            Text("${account.name} ${mask(account.lastFour)}", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Medium)
            MaskedAmount(
                account.balanceMinor,
                flags.reveal,
                flags.onToggleReveal,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

/** Shows pay hub. */
@Composable
fun PayHub(
    flags: HubFlags,
    shortcuts: Result<List<PayShortcut>>?,
    onProfile: () -> Unit,
    onOpen: (Screen) -> Unit,
) {
    FeatureScaffold("Pay", onBack = null, actions = { ProfileAction(onProfile) }) {
        if (flags.offline) PaymentsOfflineNotice()
        val online = !flags.offline
        SectionCard(title = "Send and request") {
            HubLink(Icons.Outlined.QrCodeScanner, "Scan and pay", "Any UPI QR code", online) { onOpen(Screen.Scan) }
            HubLink(Icons.AutoMirrored.Outlined.Send, "Send money", "To a contact or bank account", online) { onOpen(Screen.Wallet) }
            HubLink(Icons.AutoMirrored.Outlined.CallSplit, "Split a bill", "Request equal shares by UPI", online) {
                onOpen(Screen.SplitBill())
            }
            HubLink(Icons.Outlined.QrCode2, "UPI", "Request with QR or UPI ID", online) { onOpen(Screen.Banking) }
            HubLink(Icons.Outlined.People, "Beneficiaries", "New payees cool for 30 minutes", online) { onOpen(Screen.Banking) }
        }
        SectionCard(title = "Bills and recharges") {
            HubLink(Icons.AutoMirrored.Outlined.ReceiptLong, "Pay bills", "Bills due this month", online) {
                onOpen(Screen.Pos)
            }
        }
        SectionCard(title = "Recent people") {
            when {
                shortcuts == null -> Text("Loading people from payments…")
                shortcuts.isFailure -> StatusBanner("Payments couldn't load.", BannerTone.WARNING)
                shortcuts.getOrThrow().isEmpty() -> Text("No recent people.")
                else -> shortcuts.getOrThrow().forEach { person ->
                    ListRow(
                        Icons.Outlined.AccountCircle,
                        person.name,
                        subtitle = person.handle,
                        onClick = { onOpen(Screen.PayTo(person.handle, 0L)) }.takeIf { online },
                    )
                }
            }
        }
    }
}

/** Shows insights hub. */
@Composable
fun InsightsHub(
    highlights: Result<List<SpendHighlight>>?,
    onProfile: () -> Unit,
    onOpen: (Screen) -> Unit,
) {
    FeatureScaffold("Insights", onBack = null, actions = { ProfileAction(onProfile) }) {
        SectionCard(title = "This month") {
            when {
                highlights == null -> Text("Loading insights…")
                highlights.isFailure -> StatusBanner("Insights couldn't load.", BannerTone.WARNING)
                highlights.getOrThrow().isEmpty() -> Text("No spending highlights.")
                else -> highlights.getOrThrow().forEach { line ->
                    ListRow(
                        Icons.Outlined.PieChart,
                        line.label,
                        trailingContent = { AmountText(line.spentMinor, MoneyDirection.DEBIT, label = line.label) },
                    )
                }
            }
        }
        SectionCard(title = "Spending") {
            HubLink(Icons.Outlined.PieChart, "Budgets", "Monthly limits by category") { onOpen(Screen.Budgets) }
            HubLink(Icons.AutoMirrored.Outlined.ShowChart, "Charts", "By category and by month") { onOpen(Screen.Charts) }
        }
        SectionCard(title = "Markets") {
            HubLink(Icons.AutoMirrored.Outlined.ShowChart, "Watchlist", "Live USD/INR ticker") { onOpen(Screen.Ticker) }
            HubLink(Icons.Outlined.CurrencyExchange, "FX alerts", "Notify me when a rate crosses my target") { onOpen(Screen.FxAlerts) }
            HubLink(Icons.Outlined.NotificationsActive, "Notification centre", "Alerts, payments and security") {
                onOpen(Screen.Notifications)
            }
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
