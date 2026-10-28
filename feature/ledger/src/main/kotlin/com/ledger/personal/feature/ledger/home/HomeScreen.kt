package com.ledger.personal.feature.ledger.home

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.paging.PagingData
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeLayout
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.sync.SyncPhase
import com.ledger.personal.ui.components.BannerTone
import com.ledger.personal.ui.components.IconBadge
import com.ledger.personal.ui.components.MaskedAmount
import com.ledger.personal.ui.components.OfflineBanner
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.components.SkeletonRows
import com.ledger.personal.ui.components.StatusBanner
import com.ledger.personal.ui.components.SyncIndicator
import com.ledger.personal.ui.components.SyncState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

/** Shows the home screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    name: String,
    balanceMinor: Long,
    revealBalance: Boolean,
    offline: Boolean,
    onToggleReveal: () -> Unit,
    onScanClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
                    IconButton(onClick = onAccountClick) { Icon(Icons.Outlined.AccountCircle, contentDescription = "Account & Security") }
                    IconButton(onClick = viewModel::refresh) { Icon(Icons.Outlined.Refresh, contentDescription = "Sync now") }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 840.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (offline) {
                    item(key = "offline_banner", contentType = "banner") { OfflineBanner("a few minutes ago") }
                }
                if (!state.layoutReady) {
                    item(key = "skeleton_rows", contentType = "skeleton") { SkeletonRows() }
                } else {
                    state.layoutMessage?.let { message ->
                        item(key = "layout_message", contentType = "banner") {
                            StatusBanner(message, BannerTone.WARNING, title = "Dashboard Notice")
                        }
                    }
                    state.sections.forEachIndexed { index, section ->
                        when (section) {
                            HomeSection.Balance -> {
                                item(key = "section_balance_$index", contentType = "balance_summary") {
                                    SummaryCard(balanceMinor, revealBalance, onToggleReveal)
                                }
                                item(key = "section_scan_and_pay_$index", contentType = "scan_and_pay") {
                                    SectionCard(title = stringResource(AppStrings.SECTION_SCAN_AND_PAY)) {
                                        Text(
                                            "Pay anyone instantly using UPI QR code or phone number.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        PrimaryButton(
                                            text = stringResource(AppStrings.ACTION_SCAN_QR),
                                            icon = Icons.Outlined.QrCodeScanner,
                                            onClick = onScanClick,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                                item(key = "section_account_$index", contentType = "account_card") {
                                    SectionCard(title = "Account & Security") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = name.ifBlank { "Personal Account" },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                                Text(
                                                    text = "Manage security, lock app, or log out",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                            OutlinedButton(onClick = onAccountClick) {
                                                Text("Manage")
                                            }
                                        }
                                    }
                                }
                                if (state.promotions.isNotEmpty()) {
                                    item(key = "section_promotions_$index", contentType = "promotions_carousel") {
                                        PromotionsHorizontalSection(
                                            promotions = state.promotions,
                                            onOfferClick = { offer ->
                                                val query = Uri.encode(offer.pitch)
                                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
                                                try {
                                                    context.startActivity(intent)
                                                } catch (_: ActivityNotFoundException) {
                                                    // Ignore if browser not found
                                                }
                                            },
                                        )
                                    }
                                }
                            }
                            is HomeSection.Note -> item(
                                key = "section_note_${section.title}_$index",
                                contentType = "note_card",
                            ) {
                                SectionCard(title = section.title.ifBlank { null }) { Text(section.body) }
                            }
                            is HomeSection.Shortcuts, is HomeSection.Activity -> Unit
                        }
                    }
                }
            }
        }
    }
}

/** Shows summary card with current account balance. */
@Composable
private fun SummaryCard(balanceMinor: Long, reveal: Boolean, onToggle: () -> Unit) {
    val asOfTime = remember {
        Instant.now().atZone(ZoneId.systemDefault()).format(timeFormat)
    }
    SectionCard {
        Text("Savings •• 4821", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MaskedAmount(balanceMinor, reveal, onToggle)
        Text(
            "Available balance · as of $asOfTime",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Shows promotions features in a horizontal scrollable view. */
@Composable
private fun PromotionsHorizontalSection(
    promotions: List<PromoOffer>,
    onOfferClick: (PromoOffer) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(AppStrings.SECTION_OFFERS), style = MaterialTheme.typography.titleSmall)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(
                items = promotions,
                key = { it.id },
                contentType = { "promo_offer" },
            ) { offer ->
                androidx.compose.material3.Surface(
                    onClick = { onOfferClick(offer) },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.width(240.dp),
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconBadge(Icons.Outlined.LocalOffer, size = 32)
                            Text(
                                "PROMO",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            offer.pitch,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                        )
                        if (offer.until.isNotBlank()) {
                            Text(
                                offer.until,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Greeting. */
private fun greeting(name: String): String {
    val hour = java.time.LocalTime.now().hour
    val part = when (hour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
    return if (name.isBlank()) part else "$part, ${name.substringBefore(' ')}"
}

/** Converts this value to ui. */
private fun SyncPhase.toUi(offline: Boolean): SyncState = when {
    offline -> SyncState.WAITING
    this == SyncPhase.RUNNING -> SyncState.SYNCING
    this == SyncPhase.FAILED -> SyncState.FAILED
    this == SyncPhase.ENQUEUED || this == SyncPhase.BLOCKED -> SyncState.WAITING
    else -> SyncState.SYNCED
}

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun HomePreview() {
    PersonalLedgerTheme {
        HomeScreen(
            name = "Asha",
            balanceMinor = 2_45_678_90L,
            revealBalance = true,
            offline = false,
            onToggleReveal = {},
            viewModel = HomeViewModel(PreviewTransactions, IdleSync, PreviewBoard, PreviewPromotions),
        )
    }
}

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun HomeOfflinePreview() {
    PersonalLedgerTheme {
        HomeScreen(
            name = "Asha",
            balanceMinor = 2_45_678_90L,
            revealBalance = false,
            offline = true,
            onToggleReveal = {},
            viewModel = HomeViewModel(PreviewTransactions, IdleSync, PreviewBoard, PreviewPromotions),
        )
    }
}

private object PreviewTransactions : TransactionRepository {
    private val rows = listOf(
        Transaction("tx-1", 45_500L, "INR", "Dining", "Team lunch", 1_760_200_000_000L),
        Transaction("tx-2", 8_500_000L, "INR", "Salary", "October salary", 1_760_100_000_000L),
    )

    override fun observeTransactions() = flowOf(rows)
    override fun observeTransaction(id: String) = flowOf(rows.find { it.id == id })
    override fun pages(): Flow<PagingData<Transaction>> = flowOf(PagingData.from(rows))
    override suspend fun getTransactions(page: Int, pageSize: Int) = Result.success(rows)
    override suspend fun getTransactionById(id: String) =
        rows.find { it.id == id }?.let { Result.success(it) } ?: Result.failure(NoSuchElementException(id))
    override suspend fun createTransaction(amountInCents: Long, currency: String, category: String, description: String) =
        Result.failure<Transaction>(UnsupportedOperationException())
    override suspend fun patchTransaction(id: String, category: String?, description: String?, amountInCents: Long?) =
        Result.failure<Transaction>(UnsupportedOperationException())
    override suspend fun deleteTransaction(id: String) = Result.success(Unit)
}

private object IdleSync : SyncCoordinator {
    override fun enqueue() = Unit
    override fun observe(): Flow<SyncPhase> = flowOf(SyncPhase.SUCCEEDED)
}

private val PreviewBoard = HomeBoard {
    Result.success(
        HomeLayout(
            tabs = listOf(HomeTab("home", "Home")),
            sections = listOf(
                HomeSection.Balance,
            ),
        ),
    )
}

private val PreviewPromotions = PromotionsBackend {
    Result.success(
        listOf(
            PromoTab(
                "festive",
                "Festive",
                listOf(
                    PromoOffer("upi5", "5% back on UPI up to ₹100", "Until 31 Oct"),
                    PromoOffer("grocery", "₹50 off groceries above ₹500", "Until 20 Oct"),
                ),
            ),
        ),
    )
}

