package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.ledger.personal.data.local.TransactionDao
import com.ledger.personal.data.local.TransactionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Pages of transactions saved on this device. */
@HiltViewModel
class LedgerHistoryViewModel @Inject constructor(dao: TransactionDao) : ViewModel() {
    val pages: Flow<PagingData<TransactionEntity>> =
        Pager(PagingConfig(pageSize = 20)) { dao.pagingSource() }.flow.cachedIn(viewModelScope)
}
