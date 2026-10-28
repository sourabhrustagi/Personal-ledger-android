package com.ledger.personal.feature.ledger.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.settings.UserSettings
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.usecase.GetPagedTransactionsUseCase
import com.ledger.personal.domain.usecase.GetTransactionsUseCase
import com.ledger.personal.domain.usecase.RefreshTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** State and actions for the transaction list screen. */
@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val getTransactionsUseCase: GetTransactionsUseCase,
    private val refreshTransactionsUseCase: RefreshTransactionsUseCase,
    getPagedTransactionsUseCase: GetPagedTransactionsUseCase,
    private val syncCoordinator: SyncCoordinator,
    settings: UserSettings,
) : ViewModel() {

    constructor(
        repository: TransactionRepository,
        syncCoordinator: SyncCoordinator,
        settings: UserSettings,
    ) : this(
        GetTransactionsUseCase(repository),
        RefreshTransactionsUseCase(repository),
        GetPagedTransactionsUseCase(repository),
        syncCoordinator,
        settings,
    )

    val pages: Flow<PagingData<Transaction>> = getPagedTransactionsUseCase().cachedIn(viewModelScope)

    private val isRefreshingFlow = MutableStateFlow(false)
    private val errorMessageFlow = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TransactionListUiState> = combine(
        getTransactionsUseCase()
            .map { Result.success(it) }
            .catch { emit(Result.failure(it)) },
        isRefreshingFlow,
        errorMessageFlow,
        syncCoordinator.observe(),
        settings.lastSyncEpochMs,
    ) { itemsResult, isRefreshing, error, phase, _ ->
        TransactionListUiState(
            items = itemsResult.getOrDefault(emptyList()),
            isLoading = false,
            isRefreshing = isRefreshing,
            isEndReached = true,
            errorMessage = error ?: itemsResult.exceptionOrNull()?.message,
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

    /** Refresh transactions. */
    fun refresh() {
        syncCoordinator.enqueue()
        viewModelScope.launch {
            errorMessageFlow.value = null
            try {
                refreshTransactionsUseCase().onFailure { error ->
                    errorMessageFlow.value = error.localizedMessage ?: "Failed to refresh"
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                errorMessageFlow.value = e.localizedMessage ?: "Failed to refresh"
            }
        }
    }

    /** Triggers page load when user reaches the end of current list. */
    fun loadNextPage() = Unit
}
