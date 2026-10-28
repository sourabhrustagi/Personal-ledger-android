package com.ledger.personal.feature.ledger.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.ui.feature.LedgerHistoryScreen
import com.ledger.personal.ui.screens.home.HomeScreen
import com.ledger.personal.ui.screens.home.HomeViewModel
import com.ledger.personal.ui.screens.list.TransactionListScreen
import com.ledger.personal.ui.screens.list.TransactionListViewModel
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
    var selectedTab by remember { mutableIntStateOf(0) }
    var revealBalance by remember { mutableStateOf(false) }
    val tabs = listOf("Home", "Passbook", "Transactions")

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    val homeVm: HomeViewModel = hiltViewModel()
                    HomeScreen(
                        name = "Asha Rao (Demo)",
                        balanceMinor = MockCatalog.bankAccounts.first().balanceMinor,
                        revealBalance = revealBalance,
                        offline = false,
                        onToggleReveal = { revealBalance = !revealBalance },
                        viewModel = homeVm,
                    )
                }
                1 -> {
                    LedgerHistoryScreen(
                        onBack = null,
                        title = "Passbook",
                    )
                }
                2 -> {
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
    }
}
