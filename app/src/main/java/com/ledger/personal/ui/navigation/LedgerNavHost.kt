package com.ledger.personal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.ledger.personal.ui.screens.create.CreateTransactionScreen
import com.ledger.personal.ui.screens.create.CreateTransactionViewModel
import com.ledger.personal.ui.screens.detail.TransactionDetailScreen
import com.ledger.personal.ui.screens.detail.TransactionDetailViewModel
import com.ledger.personal.ui.screens.edit.EditTransactionScreen
import com.ledger.personal.ui.screens.edit.EditTransactionViewModel
import com.ledger.personal.ui.screens.list.TransactionListScreen
import com.ledger.personal.ui.screens.list.TransactionListViewModel

@Composable
fun LedgerNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TransactionList,
        modifier = modifier
    ) {
        // 1. Paginated List Screen
        composable<Screen.TransactionList> {
            val viewModel: TransactionListViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            TransactionListScreen(
                uiState = state,
                onRefresh = { viewModel.refresh() },
                onLoadMore = { viewModel.loadNextPage() },
                onTransactionClick = { id ->
                    navController.navigate(Screen.TransactionDetail(transactionId = id))
                },
                onCreateClick = {
                    navController.navigate(Screen.CreateTransaction)
                }
            )
        }

        // 2. Detail Screen (with deep linking support & delete-with-confirmation)
        composable<Screen.TransactionDetail>(
            deepLinks = listOf(
                navDeepLink<Screen.TransactionDetail>(basePath = "ledger://transactions"),
                navDeepLink<Screen.TransactionDetail>(basePath = "https://app.personal-ledger.com/transactions")
            )
        ) { backStackEntry ->
            val detailRoute = backStackEntry.toRoute<Screen.TransactionDetail>()
            val viewModel: TransactionDetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            TransactionDetailScreen(
                uiState = state,
                onNavigateBack = { navController.popBackStack() },
                onNavigateEdit = { id ->
                    navController.navigate(Screen.EditTransaction(transactionId = id))
                },
                onRetry = { viewModel.loadTransaction(detailRoute.transactionId) },
                onShowDeleteDialog = { show -> viewModel.setDeleteDialogVisible(show) },
                onConfirmDelete = { viewModel.confirmDelete() }
            )
        }

        // 3. Create Transaction Form Screen
        composable<Screen.CreateTransaction> {
            val viewModel: CreateTransactionViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            CreateTransactionScreen(
                uiState = state,
                onAmountChange = { viewModel.onAmountChange(it) },
                onCategoryChange = { viewModel.onCategoryChange(it) },
                onDescriptionChange = { viewModel.onDescriptionChange(it) },
                onSubmit = { viewModel.submit() },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 4. Edit Transaction Form Screen
        composable<Screen.EditTransaction> { backStackEntry ->
            val editRoute = backStackEntry.toRoute<Screen.EditTransaction>()
            val viewModel: EditTransactionViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            EditTransactionScreen(
                uiState = state,
                onAmountChange = { viewModel.onAmountChange(it) },
                onCategoryChange = { viewModel.onCategoryChange(it) },
                onDescriptionChange = { viewModel.onDescriptionChange(it) },
                onSubmit = { viewModel.submitPatch() },
                onNavigateBack = { navController.popBackStack() },
                onRetry = { viewModel.loadInitial(editRoute.transactionId) }
            )
        }
    }
}
