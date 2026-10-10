package com.ledger.personal.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.ui.app.AppViewModel
import com.ledger.personal.ui.app.CategoriesScreen
import com.ledger.personal.ui.app.ConflictScreen
import com.ledger.personal.ui.app.DisputeScreen
import com.ledger.personal.ui.app.DisputesScreen
import com.ledger.personal.ui.app.ScanScreen
import com.ledger.personal.ui.app.StatementsScreen
import com.ledger.personal.ui.app.ToolsViewModel
import com.ledger.personal.ui.feature.SplitBillScreen
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import com.ledger.personal.ui.app.HelpScreen
import com.ledger.personal.ui.app.HubFlags
import com.ledger.personal.ui.app.InsightsHub
import com.ledger.personal.ui.app.MOCK_ROLES
import com.ledger.personal.ui.app.offlineNow
import com.ledger.personal.ui.app.rememberConnected
import com.ledger.personal.ui.app.MockControlsScreen
import com.ledger.personal.ui.app.MoneyHub
import com.ledger.personal.ui.app.NotificationsScreen
import com.ledger.personal.ui.app.PayHub
import com.ledger.personal.ui.app.ProfileScreen
import com.ledger.personal.ui.app.SecurityScreen
import com.ledger.personal.ui.app.SessionState
import com.ledger.personal.ui.app.SettingsScreen
import com.ledger.personal.ui.feature.AssistantScreen
import com.ledger.personal.ui.feature.BankingScreen
import com.ledger.personal.ui.feature.BudgetsScreen
import com.ledger.personal.ui.feature.CardsScreen
import com.ledger.personal.ui.feature.ChartsScreen
import com.ledger.personal.ui.feature.DepositsScreen
import com.ledger.personal.ui.feature.FxAlertsScreen
import com.ledger.personal.ui.feature.KycScreen
import com.ledger.personal.ui.feature.LedgerHistoryScreen
import com.ledger.personal.ui.feature.LoansScreen
import com.ledger.personal.ui.feature.TickerScreen
import com.ledger.personal.ui.feature.WalletScreen
import com.ledger.personal.ui.screens.create.CreateTransactionScreen
import com.ledger.personal.ui.screens.create.CreateTransactionViewModel
import com.ledger.personal.ui.screens.detail.TransactionDetailScreen
import com.ledger.personal.ui.screens.detail.TransactionDetailViewModel
import com.ledger.personal.ui.screens.edit.EditTransactionScreen
import com.ledger.personal.ui.screens.edit.EditTransactionViewModel
import com.ledger.personal.ui.screens.home.HomeScreen
import com.ledger.personal.ui.screens.list.TransactionListScreen
import com.ledger.personal.ui.screens.list.TransactionListViewModel

/** Tab. */
private data class Tab(val route: Screen, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Screen.Home, "Home", Icons.Outlined.Home),
    Tab(Screen.MoneyHub, "Money", Icons.Outlined.AccountBalanceWallet),
    Tab(Screen.PayHub, "Pay", Icons.Outlined.QrCodeScanner),
    Tab(Screen.InsightsHub, "Insights", Icons.Outlined.Insights),
    Tab(Screen.Assistant, "Assistant", Icons.Outlined.AutoAwesome),
)

/** Shows ledger nav host. */
@Composable
fun LedgerNavHost(
    prefs: AppPrefs,
    appViewModel: AppViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val mockOffline by MockSwitches.offline.collectAsStateWithLifecycle()
    val offline = offlineNow(mockOffline, rememberConnected())
    val empty by MockSwitches.empty.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val flags = HubFlags(offline, empty, session.revealBalances || !prefs.hideBalances, appViewModel::toggleReveal)
    val tools: ToolsViewModel = hiltViewModel()
    val categoriesReady by appViewModel.categoriesReady.collectAsStateWithLifecycle()
    val categories = if (categoriesReady) prefs.categories else emptyList()
    val defaultCategories = if (categoriesReady) prefs.defaultCategories else emptyList()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (tabs.any { tab -> destination?.hasRoute(tab.route::class) == true }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true,
                            onClick = { navController.navigateTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home,
            modifier = Modifier.padding(inner).consumeWindowInsets(inner),
        ) {
            tabRoutes(navController, prefs, flags, tools)
            ledgerRoutes(navController, categories, categoriesLoading = !categoriesReady)
            productRoutes(navController, offline)
            accountRoutes(navController, prefs, session, appViewModel)
            toolRoutes(navController, tools, appViewModel, categories, defaultCategories, !categoriesReady)
        }
    }
}

/** Navigates to tab. */
private fun NavHostController.navigateTab(route: Screen) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Tab routes. */
private fun NavGraphBuilder.tabRoutes(nav: NavHostController, prefs: AppPrefs, flags: HubFlags, tools: ToolsViewModel) {
    composable<Screen.Home> {
        val conflicts by tools.inbox.conflicts.collectAsStateWithLifecycle()
        HomeScreen(
            name = prefs.name,
            balanceMinor = if (flags.empty) 0L else MockCatalog.bankAccounts.first().balanceMinor,
            revealBalance = flags.reveal,
            offline = flags.offline,
            onToggleReveal = flags.onToggleReveal,
            onProfile = { nav.navigate(Screen.Profile) },
            onOpen = { nav.navigate(Screen.TransactionDetail(it)) },
            onCreate = { nav.navigate(Screen.CreateTransaction) },
            onSeeAll = { nav.navigate(Screen.TransactionList) },
            onQuickAction = { action ->
                nav.navigate(
                    when (action) {
                        "send" -> Screen.Wallet
                        "upi" -> Screen.Scan
                        "budgets" -> Screen.Budgets
                        else -> Screen.Banking
                    },
                )
            },
            conflicts = conflicts.size,
            onConflicts = { nav.navigate(Screen.Conflicts) },
        )
    }
    composable<Screen.MoneyHub> { MoneyHub(flags, { nav.navigate(Screen.Profile) }, nav::navigate) }
    composable<Screen.PayHub> { PayHub(flags, { nav.navigate(Screen.Profile) }, nav::navigate) }
    composable<Screen.InsightsHub> { InsightsHub({ nav.navigate(Screen.Profile) }, nav::navigate) }
    composable<Screen.Assistant> {
        AssistantScreen(onBack = null, onReviewPayment = { nav.navigate(Screen.Wallet) }, offline = flags.offline)
    }
}

/** List route. */
private fun NavGraphBuilder.listRoute(nav: NavHostController) {
    composable<Screen.TransactionList> {
        val viewModel: TransactionListViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        TransactionListScreen(
            uiState = state,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadNextPage,
            onTransactionClick = { nav.navigate(Screen.TransactionDetail(transactionId = it)) },
            onCreateClick = { nav.navigate(Screen.CreateTransaction) },
            onProductsClick = { nav.popBackStack() },
            paging = viewModel.pages.collectAsLazyPagingItems(),
        )
    }
}

/** Ledger routes. */
private fun NavGraphBuilder.ledgerRoutes(nav: NavHostController, categories: List<String>, categoriesLoading: Boolean) {
    listRoute(nav)
    composable<Screen.TransactionDetail>(
        deepLinks = listOf(
            navDeepLink<Screen.TransactionDetail>(basePath = "ledger://transactions"),
            navDeepLink<Screen.TransactionDetail>(basePath = "https://app.personal-ledger.com/transactions"),
        ),
    ) { backStackEntry ->
        val route = backStackEntry.toRoute<Screen.TransactionDetail>()
        val viewModel: TransactionDetailViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        TransactionDetailScreen(
            uiState = state,
            onNavigateBack = { nav.popBackStack() },
            onNavigateEdit = { nav.navigate(Screen.EditTransaction(transactionId = it)) },
            onRetry = { viewModel.loadTransaction(route.transactionId) },
            onShowDeleteDialog = viewModel::setDeleteDialogVisible,
            onConfirmDelete = viewModel::confirmDelete,
            actions = { tx -> DetailActions(nav, tx) },
        )
    }
    composable<Screen.CreateTransaction> {
        val viewModel: CreateTransactionViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        CreateTransactionScreen(
            uiState = state,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSubmit = viewModel::submit,
            onNavigateBack = { nav.popBackStack() },
            quickCategories = categories,
            categoriesLoading = categoriesLoading,
        )
    }
    composable<Screen.EditTransaction> { backStackEntry ->
        val route = backStackEntry.toRoute<Screen.EditTransaction>()
        val viewModel: EditTransactionViewModel = hiltViewModel()
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        EditTransactionScreen(
            uiState = state,
            onAmountChange = viewModel::onAmountChange,
            onCategoryChange = viewModel::onCategoryChange,
            onDescriptionChange = viewModel::onDescriptionChange,
            onSubmit = viewModel::submitPatch,
            onNavigateBack = { nav.popBackStack() },
            onRetry = { viewModel.loadInitial(route.transactionId) },
            quickCategories = categories,
            categoriesLoading = categoriesLoading,
        )
    }
}

/** Product routes. */
private fun NavGraphBuilder.productRoutes(nav: NavHostController, offline: Boolean) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<Screen.LedgerHistory> { LedgerHistoryScreen(onBack = back) }
    composable<Screen.Charts> { ChartsScreen(onBack = back) }
    composable<Screen.Budgets> { BudgetsScreen(onBack = back, onInsights = { nav.navigate(Screen.Charts) }) }
    composable<Screen.FxAlerts> { FxAlertsScreen(onBack = back) }
    composable<Screen.Ticker> { TickerScreen(onBack = back) }
    composable<Screen.Kyc> { KycScreen(onBack = back, liveCapture = BuildConfig.FLAVOR != "mock") }
    composable<Screen.Wallet> { WalletScreen(onBack = back, debugStepUp = BuildConfig.DEBUG, offline = offline) }
    composable<Screen.Banking> { BankingScreen(onBack = back) }
    composable<Screen.Cards> { CardsScreen(onBack = back) }
    composable<Screen.Loans> { LoansScreen(onBack = back) }
    composable<Screen.Deposits> { DepositsScreen(onBack = back) }
}

/** Account routes. */
private fun NavGraphBuilder.accountRoutes(nav: NavHostController, prefs: AppPrefs, session: SessionState, app: AppViewModel) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<Screen.Profile> {
        ProfileScreen(
            prefs = prefs,
            session = session,
            roles = MOCK_ROLES,
            showMockControls = BuildConfig.DEBUG && BuildConfig.FLAVOR == "mock",
            onBack = back,
            onMode = app::setMode,
            onOpen = nav::navigate,
            onLock = app::lock,
            onLogOut = app::logOut,
        )
    }
    composable<Screen.Settings> { SettingsScreen(prefs, back, { app.setTheme(it) }, { app.setHideBalances(it) }) }
    composable<Screen.Security> {
        val context = LocalContext.current
        SecurityScreen(
            prefs = prefs,
            onBack = back,
            onBiometric = { app.setBiometric(it) },
            onChangeMpin = { app.changeMpin(it) },
            onSignOutEverywhere = { app.signOutEverywhere() },
            onBind = { done -> app.bindDevice(context, done) },
        )
    }
    composable<Screen.Notifications> { NotificationsScreen(onBack = back) }
    composable<Screen.Help> { HelpScreen(onBack = back) }

}

/** Tool routes. */
private fun NavGraphBuilder.toolRoutes(
    nav: NavHostController,
    tools: ToolsViewModel,
    app: AppViewModel,
    categories: List<String>,
    defaults: List<String>,
    categoriesLoading: Boolean,
) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<Screen.Statements> { StatementsScreen(onBack = back, tools = tools) }
    composable<Screen.Categories> {
        CategoriesScreen(categories, back, defaults, loading = categoriesLoading) { app.setCategories(it) }
    }
    composable<Screen.Dispute> { entry ->
        DisputeScreen(entry.toRoute<Screen.Dispute>().transactionId, tools, back) { nav.navigate(Screen.Disputes) }
    }
    composable<Screen.Disputes> { DisputesScreen(tools, back) }
    composable<Screen.Conflicts> { ConflictScreen(tools, back) }
    composable<Screen.MockControls> { MockControlsScreen(onBack = back, onSimulateConflict = tools::simulateConflict) }
    composable<Screen.Scan> {
        ScanScreen(liveCamera = BuildConfig.FLAVOR != "mock", onBack = back) { target ->
            nav.navigate(Screen.PayTo(target.vpa, target.amountMinor)) { popUpTo<Screen.Scan> { inclusive = true } }
        }
    }
    composable<Screen.PayTo> { entry ->
        val route = entry.toRoute<Screen.PayTo>()
        WalletScreen(onBack = back, debugStepUp = BuildConfig.DEBUG, prefillHandle = route.handle, prefillMinor = route.amountMinor)
    }
    composable<Screen.SplitBill> { entry ->
        val route = entry.toRoute<Screen.SplitBill>()
        SplitBillScreen(onBack = back, initialMinor = route.amountMinor, note = route.note)
    }
}

/** Shows detail actions. */
@Composable
private fun DetailActions(nav: NavHostController, tx: Transaction) {
    OutlinedButton(onClick = { nav.navigate(Screen.SplitBill(tx.amountInCents, tx.description)) }, modifier = Modifier.fillMaxWidth()) {
        Text("Split this bill")
    }
    OutlinedButton(onClick = { nav.navigate(Screen.Dispute(tx.id)) }, modifier = Modifier.fillMaxWidth()) {
        Text("Report a problem")
    }
}
