package com.ledger.personal.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.HomeTile
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
    val tiles: List<HomeTile> = emptyList(),
    val tilesMessage: String? = null,
)

private const val RECENT_LIMIT = 30
const val UNDO_WINDOW_MS = 4_000L

/** State and actions for the home screen. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: TransactionRepository,
    private val sync: SyncCoordinator,
    board: HomeBoard,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val category = MutableStateFlow<String?>(null)
    private val pending = MutableStateFlow<Transaction?>(null)
    private val tiles = MutableStateFlow<List<HomeTile>>(emptyList())
    private val tilesMessage = MutableStateFlow<String?>(null)
    private var deleteJob: Job? = null

    init {
        viewModelScope.launch {
            board.tiles().fold(
                onSuccess = { tiles.value = it },
                onFailure = { tilesMessage.value = "The home layout couldn't load." },
            )
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        combine(repository.observeTransactions(), query, category, pending, sync.observe()) { all, q, cat, held, phase ->
            LedgerSlice(all, q, cat, held, phase)
        },
        tiles,
        tilesMessage,
    ) { slice, boardTiles, message ->
        val (all, q, cat, held, phase) = slice
        val visible = all.filter { it.id != held?.id }
        val month = YearMonth.now()
        val thisMonth = visible.filter { it.currency == "INR" && YearMonth.from(it.date()) == month }
        val filtered = visible.filter { tx ->
            (cat == null || tx.category == cat) &&
                (q.isBlank() || tx.description.contains(q, true) || tx.category.contains(q, true))
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
            tiles = boardTiles,
            tilesMessage = message,
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
        pending.value?.let { previous -> viewModelScope.launch { repository.deleteTransaction(previous.id) } }
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
            repository.deleteTransaction(tx.id)
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

/** Date. */
private fun Transaction.date(): LocalDate = Instant.ofEpochMilli(timestampEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()

private val dayFormat = DateTimeFormatter.ofPattern("EEE, d MMM")

/** Day label. */
fun dayLabel(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    else -> date.format(dayFormat)
}
