package com.ledger.personal.ui.screens.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.paging.PagingData
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeLayout
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.HomeTile
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.SyncPhase
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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
            onProfile = {},
            onOpen = {},
            onCreate = {},
            onSeeAll = {},
            onQuickAction = {},
            viewModel = HomeViewModel(PreviewTransactions, IdleSync, PreviewBoard),
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

private object PreviewBoard : HomeBoard {
    override suspend fun layout() = Result.success(
        HomeLayout(
            tabs = listOf(HomeTab("home", "Home")),
            sections = listOf(
                HomeSection.Balance,
                HomeSection.Shortcuts(listOf(HomeTile("send", "Send"), HomeTile("bills", "Bills"))),
                HomeSection.Activity("Recent"),
            ),
        ),
    )
}
