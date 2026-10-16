package com.ledger.personal.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.data.tools.DisputeDesk
import com.ledger.personal.domain.board.PayShortcut
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.SpendHighlight
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@PreviewLightDark
@Composable
private fun LoginPreview() {
    PersonalLedgerTheme {
        MpinLoginScreen(
            name = "Asha",
            biometric = false,
            session = SessionState(),
            onPin = { _, done -> done(true) },
            onBiometric = {},
            onReset = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun OnboardingPreview() {
    PersonalLedgerTheme { OnboardingFlow(onFinish = { _, _, _, _, _ -> }, onBind = {}) }
}

@PreviewLightDark
@Composable
private fun ProfilePreview() {
    PersonalLedgerTheme {
        ProfileScreen(AppPrefs(name = "Asha Rao"), showMockControls = true, onBack = {}, onOpen = {}, onLock = {}, onLogOut = {})
    }
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    PersonalLedgerTheme { SettingsScreen(AppPrefs(name = "Asha Rao"), onBack = {}, onTheme = {}, onHideBalances = {}) }
}

@PreviewLightDark
@Composable
private fun SecurityPreview() {
    PersonalLedgerTheme {
        SecurityScreen(AppPrefs(name = "Asha Rao"), onBack = {}, onBiometric = {}, onChangeMpin = {}, onSignOutEverywhere = {}, onBind = {})
    }
}

@PreviewLightDark
@Composable
private fun NotificationsPreview() {
    PersonalLedgerTheme { NotificationsScreen(onBack = {}) }
}

@PreviewLightDark
@Composable
private fun HelpPreview() {
    PersonalLedgerTheme { HelpScreen(onBack = {}) }
}

@PreviewLightDark
@Composable
private fun MockControlsPreview() {
    PersonalLedgerTheme { MockControlsScreen(onBack = {}, onSimulateConflict = {}) }
}

@PreviewLightDark
@Composable
private fun MoneyHubPreview() {
    PersonalLedgerTheme { MoneyHub(previewFlags, onProfile = {}, onOpen = {}) }
}

@PreviewLightDark
@Composable
private fun PayHubPreview() {
    PersonalLedgerTheme {
        PayHub(previewFlags, Result.success(listOf(PayShortcut("Asha Rao", "asha@okaxis"))), onProfile = {}, onOpen = {})
    }
}

@PreviewLightDark
@Composable
private fun InsightsHubPreview() {
    PersonalLedgerTheme {
        InsightsHub(Result.success(listOf(SpendHighlight("Groceries", 82_000L))), onProfile = {}, onOpen = {})
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
private fun ForceUpdatePreview() {
    PersonalLedgerTheme { ForceUpdateScreen("2.0.0") }
}

@PreviewLightDark
@Composable
private fun CategoriesPreview() {
    PersonalLedgerTheme { CategoriesScreen(listOf("Groceries", "Dining", "Utilities"), onBack = {}, onSave = {}) }
}

@PreviewLightDark
@Composable
private fun StatementsPreview() {
    PersonalLedgerTheme { StatementsScreen(onBack = {}, tools = previewTools) }
}

@PreviewLightDark
@Composable
private fun DisputePreview() {
    PersonalLedgerTheme { DisputeScreen("tx-1", previewTools, onBack = {}, onTrack = {}) }
}

@PreviewLightDark
@Composable
private fun DisputesPreview() {
    PersonalLedgerTheme { DisputesScreen(previewTools, onBack = {}) }
}

@PreviewLightDark
@Composable
private fun ConflictPreview() {
    PersonalLedgerTheme { ConflictScreen(previewTools, onBack = {}) }
}

private val previewFlags = HubFlags(offline = false, empty = false, reveal = true, onToggleReveal = {})

private val previewTools = ToolsViewModel(PreviewTransactions, DisputeDesk(null), ConflictInbox(null))

private object PreviewTransactions : TransactionRepository {
    private val rows = listOf(Transaction("tx-1", 50_000L, "INR", "Utilities", "Electricity", 1_760_200_000_000L))

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
