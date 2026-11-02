package com.ledger.personal.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.usecase.GetPagedTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Pages of transactions saved on this device with access to underlying repository. */
@HiltViewModel
class LedgerHistoryViewModel @Inject constructor(
    getPagedTransactionsUseCase: GetPagedTransactionsUseCase,
    val repository: TransactionRepository,
) : ViewModel() {
    val pages: Flow<PagingData<Transaction>> =
        getPagedTransactionsUseCase().cachedIn(viewModelScope)
}
