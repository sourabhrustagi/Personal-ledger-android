package com.ledger.personal.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeLayout
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.SyncPhase
import com.ledger.personal.ui.components.MaskedAmount
import com.ledger.personal.ui.components.OfflineBanner
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.components.SyncIndicator
import com.ledger.personal.ui.components.SyncState
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import androidx.paging.PagingData


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
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(greeting(name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        SyncIndicator(state.sync.toUi(offline))
                    }
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
                item(key = "section_balance", contentType = "balance_summary") {
                    SummaryCard(balanceMinor, revealBalance, onToggleReveal)
                }
            }
        }
    }
}

/** Shows summary card with account number and balance only. */
@Composable
private fun SummaryCard(balanceMinor: Long, reveal: Boolean, onToggle: () -> Unit) {
    SectionCard {
        Text(
            text = "Savings A/C •• 4821",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MaskedAmount(balanceMinor, reveal, onToggle)
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
            tabs = listOf(
                HomeTab("home", "Home"),
                HomeTab("pay", "Pay"),
                HomeTab("passbook", "Passbook"),
                HomeTab("offers", "Offers"),
                HomeTab("account", "Account"),
            ),
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
