package com.ledger.personal.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.sync.SyncPhase
import com.ledger.personal.ui.feature.AmountText
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.EmptyState
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.MaskedAmount
import com.ledger.personal.ui.feature.MoneyDirection
import com.ledger.personal.ui.feature.OfflineBanner
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.SkeletonRows
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.SyncIndicator
import com.ledger.personal.ui.feature.SyncState
import com.ledger.personal.ui.feature.categoryIcon
import com.ledger.personal.ui.feature.formatMoney
import com.ledger.personal.ui.theme.LedgerTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class QuickAction(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val quickActions = listOf(
    QuickAction("send", "Send", Icons.AutoMirrored.Outlined.Send),
    QuickAction("upi", "UPI", Icons.Outlined.QrCode2),
    QuickAction("bills", "Pay bills", Icons.AutoMirrored.Outlined.ReceiptLong),
    QuickAction("budgets", "Budgets", Icons.Outlined.PieChart),
)

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    name: String,
    balanceMinor: Long,
    revealBalance: Boolean,
    offline: Boolean,
    onToggleReveal: () -> Unit,
    onProfile: () -> Unit,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onSeeAll: () -> Unit,
    onQuickAction: (String) -> Unit,
    conflicts: Int = 0,
    onConflicts: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.pendingDelete?.id) {
        val held = state.pendingDelete ?: return@LaunchedEffect
        val result = snackbar.showSnackbar("Deleted ${held.description}", "Undo", duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete() else viewModel.commitDelete()
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(greeting(name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        SyncIndicator(state.sync.toUi(offline))
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Outlined.Refresh, contentDescription = "Sync now") }
                    IconButton(onClick = onProfile) { Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) { Icon(Icons.Outlined.Add, contentDescription = "Add transaction") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (offline) item { OfflineBanner("a few minutes ago") }
            if (conflicts > 0) {
                item {
                    androidx.compose.material3.Surface(onClick = onConflicts, shape = MaterialTheme.shapes.medium) {
                        StatusBanner(
                            "$conflicts transaction${if (conflicts == 1) "" else "s"} changed here and on another device. Tap to choose what to keep.",
                            BannerTone.WARNING,
                            title = "Sync conflict",
                        )
                    }
                }
            }
            item { SummaryCard(balanceMinor, revealBalance, onToggleReveal, state) }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(quickActions) { action -> QuickActionTile(action) { onQuickAction(action.id) } }
                }
            }
            item {
                OutlinedTextField(
                    state.query,
                    viewModel::onQuery,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    placeholder = { Text("Search merchant or category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.categories) { cat ->
                        FilterChip(state.category == cat, { viewModel.onCategory(cat) }, label = { Text(cat) })
                    }
                }
            }
            transactionsSection(state, onOpen, onCreate, onSeeAll, viewModel)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.transactionsSection(
    state: HomeUiState,
    onOpen: (String) -> Unit,
    onCreate: () -> Unit,
    onSeeAll: () -> Unit,
    viewModel: HomeViewModel,
) {
    when {
        state.loading -> item { SkeletonRows() }
        state.groups.isEmpty() && (state.query.isNotBlank() || state.category != null) -> item {
            EmptyState(Icons.Outlined.Search, "No matches", "Try another merchant name or clear the category filter.")
        }
        state.groups.isEmpty() -> item {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EmptyState(Icons.AutoMirrored.Outlined.ReceiptLong, "No transactions yet", "Add your first expense or income to start tracking.")
                TextButton(onClick = onCreate) { Text("Add transaction") }
            }
        }
        else -> {
            state.groups.forEach { group ->
                item(key = "h-${group.label}") {
                    Text(group.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(group.items, key = { it.id }) { tx ->
                    SwipeToDelete(onDelete = { viewModel.delete(tx) }) { TransactionRow(tx) { onOpen(tx.id) } }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onSeeAll) { Text("See all ${state.total} transactions") }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(balanceMinor: Long, reveal: Boolean, onToggle: () -> Unit, state: HomeUiState) {
    SectionCard {
        Text("Savings •• 4821", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MaskedAmount(balanceMinor, reveal, onToggle)
        Text(
            "Available balance · as of ${Instant.now().atZone(ZoneId.systemDefault()).format(timeFormat)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Money in this month", style = MaterialTheme.typography.bodySmall)
                AmountText(state.monthInMinor, MoneyDirection.CREDIT, label = "this month")
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Money out this month", style = MaterialTheme.typography.bodySmall)
                AmountText(state.monthOutMinor, MoneyDirection.DEBIT, label = "this month")
            }
        }
        val net = state.monthInMinor - state.monthOutMinor
        StatusBanner(
            if (net >= 0) "You've saved ${formatMoney(net)} so far this month." else "You've spent ${formatMoney(-net)} more than you earned.",
            if (net >= 0) BannerTone.SUCCESS else BannerTone.WARNING,
        )
    }
}

@Composable
private fun QuickActionTile(action: QuickAction, onClick: () -> Unit) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(action.icon, size = 40)
            Text(action.label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
fun TransactionRow(tx: Transaction, onClick: () -> Unit) {
    val credit = tx.isCredit()
    ListRow(
        icon = categoryIcon(tx.category),
        title = tx.description,
        subtitle = "${tx.category} · ${Instant.ofEpochMilli(tx.timestampEpochMs).atZone(ZoneId.systemDefault()).format(timeFormat)}",
        iconTint = if (credit) LedgerTheme.colors.success else MaterialTheme.colorScheme.primary,
        onClick = onClick,
        trailing = null,
        modifier = Modifier.background(MaterialTheme.colorScheme.background),
        trailingContent = {
            AmountText(
                tx.amountInCents,
                if (credit) MoneyDirection.CREDIT else MoneyDirection.DEBIT,
                currency = tx.currency,
                label = "${tx.category}, ${tx.description}",
            )
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDelete(onDelete: () -> Unit, content: @Composable () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            value == SwipeToDismissBoxValue.EndToStart
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Delete", color = MaterialTheme.colorScheme.onErrorContainer)
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
        },
    ) { content() }
}

private fun greeting(name: String): String {
    val hour = java.time.LocalTime.now().hour
    val part = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isBlank()) part else "$part, ${name.substringBefore(' ')}"
}

private fun SyncPhase.toUi(offline: Boolean): SyncState = when {
    offline -> SyncState.WAITING
    this == SyncPhase.RUNNING -> SyncState.SYNCING
    this == SyncPhase.FAILED -> SyncState.FAILED
    this == SyncPhase.ENQUEUED || this == SyncPhase.BLOCKED -> SyncState.WAITING
    else -> SyncState.SYNCED
}
