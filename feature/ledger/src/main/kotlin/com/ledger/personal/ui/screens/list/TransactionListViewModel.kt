package com.ledger.personal.ui.screens.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.cachedIn
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.settings.UserSettings
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.label
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

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
            syncStatus = phase.label(),
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        TransactionListUiState(),
    )

    init {
        syncCoordinator.enqueue()
    }

    fun loadInitialPage() {
        syncCoordinator.enqueue()
    }

    fun refresh() {
        syncCoordinator.enqueue()
    }

    fun loadNextPage() = Unit
}
