package com.ledger.personal.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.settings.UserSettings
import com.ledger.personal.domain.sync.SyncCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** State and actions for the transaction list screen. */
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    repository: TransactionRepository,
    private val syncCoordinator: SyncCoordinator,
    settings: UserSettings,
) : ViewModel() {

    val pages = repository.pages().cachedIn(viewModelScope)

    val uiState: StateFlow<TransactionListUiState> = combine(
        syncCoordinator.observe(),
        settings.lastSyncEpochMs,
    ) { phase, _ ->
        TransactionListUiState(
            isLoading = false,
            isEndReached = true,
            syncStatus = phase.label,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TransactionListUiState(),
    )

    init {
        syncCoordinator.enqueue()
    }

    /** Loads initial page. */
    fun loadInitialPage() {
        syncCoordinator.enqueue()
    }

    /** Refresh. */
    fun refresh() {
        syncCoordinator.enqueue()
    }

    /** Loads next page. */
    fun loadNextPage() = Unit
}
