package com.ledger.personal.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeSection
import com.ledger.personal.domain.board.HomeTab
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.SyncPhase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import com.ledger.personal.domain.usecase.DeleteTransactionUseCase
import com.ledger.personal.domain.usecase.GetTransactionsUseCase

const val INCOME_CATEGORY = "Salary"

/** Reports whether this is credit. */
fun Transaction.isCredit(): Boolean = category == INCOME_CATEGORY

/** Day group. */
data class DayGroup(val label: String, val items: List<Transaction>)

/** Values rendered by the home screen. */
data class HomeUiState(
    val loading: Boolean = true,
    val query: String = "",
    val category: String? = null,
    val categories: List<String> = emptyList(),
    val groups: List<DayGroup> = emptyList(),
    val monthInMinor: Long = 0,
    val monthOutMinor: Long = 0,
    val sync: SyncPhase = SyncPhase.IDLE,
    val pendingDelete: Transaction? = null,
    val total: Int = 0,
    val tabs: List<HomeTab> = emptyList(),
    val sections: List<HomeSection> = emptyList(),
    val layoutReady: Boolean = false,
    val layoutMessage: String? = null,
    val promotions: List<PromoOffer> = emptyList(),
)

private const val RECENT_LIMIT = 30

/** Balance stays on screen when the dashboard cannot be loaded. */
private val essentialHome = listOf(HomeSection.Balance)

/** Home stays reachable when the dashboard cannot be loaded. */
private val essentialTabs = listOf(HomeTab("home", "Home"))
const val UNDO_WINDOW_MS = 4_000L

private val PreviewPromotionsBackend = PromotionsBackend { Result.success(emptyList()) }

/** State and actions for the home screen. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val deleteTransactionUseCase: DeleteTransactionUseCase,
    private val sync: SyncCoordinator,
    board: HomeBoard,
    promotionsBackend: PromotionsBackend = PreviewPromotionsBackend,
) : ViewModel() {

    constructor(
        repository: TransactionRepository,
        sync: SyncCoordinator,
        board: HomeBoard,
        promotionsBackend: PromotionsBackend = PreviewPromotionsBackend,
    ) : this(
        GetTransactionsUseCase(repository),
        DeleteTransactionUseCase(repository),
        sync,
        board,
        promotionsBackend,
    )
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<String?>(null)
    private val pending = MutableStateFlow<Transaction?>(null)
    private val tabs = MutableStateFlow<List<HomeTab>>(emptyList())
    private val sections = MutableStateFlow<List<HomeSection>>(emptyList())
    private val promotions = MutableStateFlow<List<PromoOffer>>(emptyList())
    private val layoutMessage = MutableStateFlow<String?>(null)
    private val layoutReady = MutableStateFlow(false)
    private var deleteJob: Job? = null

    init {
        viewModelScope.launch {
            board.layout().fold(
                onSuccess = {
                    tabs.value = it.tabs
                    sections.value = it.sections
                },
                onFailure = {
                    layoutMessage.value = "The home layout couldn't load."
                    tabs.value = essentialTabs
                    sections.value = essentialHome
                },
            )
            layoutReady.value = true
        }
        viewModelScope.launch {
            promotionsBackend.tabs().fold(
                onSuccess = { promoTabs ->
                    promotions.value = promoTabs.flatMap { it.offers }
                },
                onFailure = {
                    promotions.value = emptyList()
                },
            )
        }
    }

    private val boardSlice = combine(tabs, sections, layoutMessage, layoutReady, promotions) { bar, board, message, ready, promoOffers ->
        BoardSlice(bar, board, message, ready, promoOffers)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(getTransactionsUseCase(), query, category, pending, sync.observe()) { all, q, cat, held, phase ->
            LedgerSlice(all, q, cat, held, phase)
        },
        boardSlice,
    ) { slice, boardState ->
        val (all, q, cat, held, phase) = slice
        val visible = all.filter { it.id != held?.id }
        val month = YearMonth.now()
        val thisMonth = visible.filter { it.currency == "INR" && YearMonth.from(it.date()) == month }
        val filtered = visible.filter {
            (cat == null || it.category == cat) &&
                (q.isBlank() || it.description.contains(q, true) || it.category.contains(q, true))
        }
        HomeUiState(
            loading = false,
            query = q,
            category = cat,
            categories = all.map { it.category }.distinct().sorted(),
            groups = filtered.take(RECENT_LIMIT).groupBy { dayLabel(it.date()) }.map { (label, items) -> DayGroup(label, items) },
            monthInMinor = thisMonth.filter { it.isCredit() }.sumOf { it.amountInCents },
            monthOutMinor = thisMonth.filterNot { it.isCredit() }.sumOf { it.amountInCents },
            sync = phase,
            pendingDelete = held,
            total = filtered.size,
            tabs = boardState.tabs,
            sections = boardState.sections,
            layoutReady = boardState.ready,
            layoutMessage = boardState.message,
            promotions = boardState.promotions,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Reacts to query. */
    fun onQuery(text: String) {
        query.value = text
    }

    /** Reacts to category. */
    fun onCategory(name: String?) {
        category.value = if (category.value == name) null else name
    }

    /** Refresh. */
    fun refresh() = sync.enqueue()

    /** Delete. */
    fun delete(tx: Transaction) {
        deleteJob?.cancel()
        pending.value?.let { previous -> viewModelScope.launch { deleteTransactionUseCase(previous.id) } }
        pending.value = tx
        deleteJob = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            commitDelete()
        }
    }

    /** Undo delete. */
    fun undoDelete() {
        deleteJob?.cancel()
        pending.value = null
    }

    /** Commit delete. */
    fun commitDelete() {
        val tx = pending.value ?: return
        deleteJob?.cancel()
        viewModelScope.launch {
            deleteTransactionUseCase(tx.id)
            pending.value = null
        }
    }
}

/** Ledger fields combined before the dashboard tiles are applied. */
private data class LedgerSlice(
    val all: List<Transaction>,
    val query: String,
    val category: String?,
    val pending: Transaction?,
    val sync: SyncPhase,
)

/** Dashboard board fields combined before merging with ledger data. */
private data class BoardSlice(
    val tabs: List<HomeTab>,
    val sections: List<HomeSection>,
    val message: String?,
    val ready: Boolean,
    val promotions: List<PromoOffer>,
)

/** Date. */
private fun Transaction.date(): LocalDate = Instant.ofEpochMilli(timestampEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")

/** Day label. */
fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(dayFormat)
}
