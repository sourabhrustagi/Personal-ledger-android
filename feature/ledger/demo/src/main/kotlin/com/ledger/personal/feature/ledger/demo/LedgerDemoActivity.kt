package com.ledger.personal.feature.ledger.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.ledger.personal.feature.ledger.list.TransactionListScreen
import com.ledger.personal.feature.ledger.list.TransactionListViewModel
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LedgerDemoActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PersonalLedgerTheme {
                LedgerDemoScreen()
            }
        }
    }
}

@Composable
fun LedgerDemoScreen() {
    Scaffold { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val listVm: TransactionListViewModel = hiltViewModel()
            val listState by listVm.uiState.collectAsStateWithLifecycle()
            TransactionListScreen(
                uiState = listState,
                onRefresh = listVm::refresh,
                onLoadMore = listVm::loadNextPage,
                paging = listVm.pages.collectAsLazyPagingItems(),
            )
        }
    }
}
