package com.ledger.personal.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.ledger.personal.core.toFormattedCurrency
import com.ledger.personal.core.toFormattedDateTime
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.usecase.GetPagedTransactionsUseCase
import com.ledger.personal.feature.history.circuit.CircuitHistoryPresenter
import com.ledger.personal.feature.history.circuit.CircuitHistoryUi
import com.ledger.personal.feature.history.decompose.DecomposeHistoryContent
import com.ledger.personal.feature.history.decompose.DefaultComponentContext
import com.ledger.personal.feature.history.decompose.DefaultDecomposeHistoryComponent
import com.ledger.personal.feature.history.mavericks.MavericksHistoryContent
import com.ledger.personal.feature.history.molecule.MoleculeHistoryContent
import com.ledger.personal.feature.history.mvikotlin.MviKotlinHistoryContent
import com.ledger.personal.feature.history.mvikotlin.MviKotlinHistoryStoreFactory
import com.ledger.personal.feature.history.orbit.OrbitHistoryContent
import com.ledger.personal.feature.history.orbit.OrbitHistoryViewModel
import com.ledger.personal.feature.history.switcher.HistoryArchitecture
import com.ledger.personal.feature.history.switcher.HistoryArchitectureHeader
import com.ledger.personal.feature.history.switcher.HistoryArchitectureSwitcher
import com.ledger.personal.feature.history.voyager.VoyagerHistoryContent
import com.ledger.personal.ui.components.EmptyState
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.ListRow
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.components.categoryIcon
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.theme.colorScheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Main Ledger History Screen featuring dynamic state architecture switching.
 * Developers can switch between Standard ViewModel, Orbit MVI, Molecule, Circuit,
 * Decompose, MVIKotlin, Voyager, and Mavericks.
 */
@Composable
fun LedgerHistoryScreen(
    onBack: (() -> Unit)? = null,
    title: String = stringResource(AppStrings.TITLE_PASSBOOK),
    initialArchitecture: HistoryArchitecture = HistoryArchitecture.STANDARD_VIEWMODEL,
    viewModel: LedgerHistoryViewModel = hiltViewModel(),
) {
    var selectedArchitecture by remember { mutableStateOf(initialArchitecture) }
    val pages = viewModel.pages.collectAsLazyPagingItems()

    FeatureScaffold(
        title = title,
        onBack = onBack,
        subtitle = "Everything saved on this device",
        scrollable = false,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HistoryArchitectureSwitcher(
                selectedArchitecture = selectedArchitecture,
                onArchitectureSelected = { selectedArchitecture = it },
            )

            HistoryArchitectureHeader(architecture = selectedArchitecture)

            Box(modifier = Modifier.weight(1f)) {
                when (selectedArchitecture) {
                    HistoryArchitecture.STANDARD_VIEWMODEL -> {
                        StandardViewModelHistoryContent(pages = pages)
                    }
                    HistoryArchitecture.ORBIT_MVI -> {
                        val orbitVm = remember(viewModel.repository) {
                            OrbitHistoryViewModel(viewModel.repository)
                        }
                        OrbitHistoryContent(viewModel = orbitVm)
                    }
                    HistoryArchitecture.CASHAPP_MOLECULE -> {
                        MoleculeHistoryContent(repository = viewModel.repository)
                    }
                    HistoryArchitecture.SLACK_CIRCUIT -> {
                        val presenter = remember(viewModel.repository) {
                            CircuitHistoryPresenter(viewModel.repository)
                        }
                        val circuitState = presenter.present()
                        val circuitUi = remember { CircuitHistoryUi() }
                        circuitUi.Content(state = circuitState, modifier = Modifier.fillMaxSize())
                    }
                    HistoryArchitecture.ARKIVANOV_DECOMPOSE -> {
                        val scope = rememberCoroutineScope()
                        val component = remember(viewModel.repository) {
                            DefaultDecomposeHistoryComponent(
                                componentContext = DefaultComponentContext(),
                                repository = viewModel.repository,
                                coroutineScope = scope,
                            )
                        }
                        DecomposeHistoryContent(component = component)
                    }
                    HistoryArchitecture.ARKIVANOV_MVIKOTLIN -> {
                        val scope = rememberCoroutineScope()
                        val store = remember(viewModel.repository) {
                            MviKotlinHistoryStoreFactory.create(
                                repository = viewModel.repository,
                                scope = scope,
                            )
                        }
                        MviKotlinHistoryContent(store = store)
                    }
                    HistoryArchitecture.VOYAGER_SCREENMODEL -> {
                        VoyagerHistoryContent(repository = viewModel.repository)
                    }
                    HistoryArchitecture.AIRBNB_MAVERICKS -> {
                        MavericksHistoryContent(repository = viewModel.repository)
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardViewModelHistoryContent(
    pages: LazyPagingItems<Transaction>,
) {
    val isInitialLoading = pages.loadState.refresh is LoadState.Loading && pages.itemCount == 0
    if (isInitialLoading) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    } else if (pages.itemCount == 0) {
        EmptyState(
            Icons.AutoMirrored.Outlined.ReceiptLong,
            stringResource(AppStrings.EMPTY_TRANSACTIONS_TITLE),
            stringResource(AppStrings.EMPTY_TRANSACTIONS_SUBTITLE),
        )
    } else {
        SectionCard(Modifier.fillMaxSize()) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(
                    count = pages.itemCount,
                    key = pages.itemKey { it.id },
                    contentType = pages.itemContentType { "passbook_row" },
                ) { index ->
                    val row = pages[index] ?: return@items
                    val formattedDate = remember(row.timestampEpochMs) { row.timestampEpochMs.toFormattedDateTime() }
                    val formattedAmount = remember(row.amountInCents, row.currency) {
                        row.amountInCents.toFormattedCurrency(row.currency)
                    }
                    ListRow(
                        icon = categoryIcon(row.category),
                        title = row.description.ifBlank { row.category },
                        subtitle = "${row.category} · $formattedDate",
                        trailing = formattedAmount,
                        trailingColor = colorScheme.onSurface,
                    )
                }
                if (pages.loadState.append is LoadState.Loading) {
                    item(key = "append_loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun LedgerHistoryPreview() {
    PersonalLedgerTheme {
        LedgerHistoryScreen(
            onBack = {},
            viewModel = LedgerHistoryViewModel(
                GetPagedTransactionsUseCase(PreviewTransactionsRepo),
                PreviewTransactionsRepo,
            ),
        )
    }
}

private val previewTransaction = Transaction("tx-1", 120_00L, "INR", "Scan & Pay", "Chai Point · QR Pay", 1_760_200_000_000L)

private object PreviewTransactionsRepo : TransactionRepository {
    override fun observeTransactions() = flowOf(listOf(previewTransaction))
    override fun observeTransaction(id: String) = flowOf(previewTransaction.takeIf { it.id == id })
    override fun pages(): Flow<PagingData<Transaction>> = flowOf(PagingData.from(listOf(previewTransaction)))
    override suspend fun getTransactions(page: Int, pageSize: Int) = Result.success(listOf(previewTransaction))
    override suspend fun getTransactionById(id: String) = Result.success(previewTransaction)
    override suspend fun createTransaction(amountInCents: Long, currency: String, category: String, description: String) =
        Result.success(previewTransaction)
    override suspend fun patchTransaction(id: String, category: String?, description: String?, amountInCents: Long?) =
        Result.success(previewTransaction)
    override suspend fun deleteTransaction(id: String) = Result.success(Unit)
}
