package com.ledger.personal.ui.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import androidx.navigation.toRoute
import androidx.paging.compose.collectAsLazyPagingItems
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.domain.model.Transaction
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.ui.app.AppViewModel
import com.ledger.personal.ui.app.DemoViewModel
import com.ledger.personal.ui.app.HubFlags
import com.ledger.personal.ui.app.offlineNow
import com.ledger.personal.ui.app.rememberConnected
import com.ledger.personal.ui.app.account.HelpScreen
import com.ledger.personal.ui.app.account.NotificationsScreen
import com.ledger.personal.ui.app.account.ProfileScreen
import com.ledger.personal.ui.app.account.SecurityScreen
import com.ledger.personal.ui.app.account.SettingsScreen
import com.ledger.personal.ui.app.hubs.PayHub
import com.ledger.personal.ui.app.hubs.PromotionsHub
import com.ledger.personal.ui.app.hubs.PromotionsViewModel
import com.ledger.personal.ui.app.scan.ScanScreen
import com.ledger.personal.ui.app.tools.CategoriesScreen
import com.ledger.personal.ui.app.tools.ConflictScreen
import com.ledger.personal.ui.app.tools.DisputeScreen
import com.ledger.personal.ui.app.tools.DisputesScreen
import com.ledger.personal.ui.app.tools.MockControlsScreen
import com.ledger.personal.ui.app.tools.StatementsScreen
import com.ledger.personal.ui.app.tools.ToolsViewModel
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import com.ledger.personal.ui.feature.BankingScreen
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
import com.ledger.personal.ui.screens.home.HomeViewModel
import com.ledger.personal.ui.screens.list.TransactionListScreen
import com.ledger.personal.ui.screens.list.TransactionListViewModel
import androidx.core.util.Consumer

/** Shows ledger nav host. */
@Composable
fun LedgerNavHost(
    prefs: AppPrefs,
    appViewModel: AppViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val session by appViewModel.session.collectAsStateWithLifecycle()
    val demo: DemoViewModel = hiltViewModel()
    val mockOffline by demo.offline.collectAsStateWithLifecycle()
    val offline = offlineNow(mockOffline, rememberConnected())
    val empty by demo.empty.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val destination = entry?.destination
    val flags = HubFlags(offline, empty, session.revealBalances || !prefs.hideBalances, appViewModel::toggleReveal)
    val tools: ToolsViewModel = hiltViewModel()
    val home: HomeViewModel = hiltViewModel()
    val homeState by home.uiState.collectAsStateWithLifecycle()
    fun routeFor(id: String): Screen? = when (id) {
        "home" -> Screen.Home
        "pay" -> Screen.PayHub
        "passbook" -> Screen.Passbook
        "accounts" -> Screen.Profile
        "offers" -> Screen.Promotions()
        else -> null
    }
    fun iconFor(id: String) = when (id) {
        "home" -> Icons.Outlined.Home
        "pay" -> Icons.Outlined.QrCodeScanner
        "passbook" -> Icons.AutoMirrored.Outlined.ReceiptLong
        "accounts" -> Icons.Outlined.AccountCircle
        "offers" -> Icons.Outlined.LocalOffer
        else -> null
    }
    val bar = homeState.tabs.mapNotNull { tab ->
        val route = routeFor(tab.id) ?: return@mapNotNull null
        val icon = iconFor(tab.id) ?: return@mapNotNull null
        BarItem(tab.title, route, icon)
    }
    val activity = LocalActivity.current as? ComponentActivity
    if (activity != null) DisposableEffect(activity, navController) {
        val listener = Consumer<Intent> { navController.handleDeepLink(it) }
        activity.addOnNewIntentListener(listener)
        onDispose { activity.removeOnNewIntentListener(listener) }
    }
    val categoriesReady by appViewModel.categoriesReady.collectAsStateWithLifecycle()
    val categories = if (categoriesReady) prefs.categories else emptyList()
    val defaultCategories = if (categoriesReady) prefs.defaultCategories else emptyList()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (bar.any { destination?.hasRoute(it.route::class) == true }) {
                NavigationBar {
                    bar.forEach { item ->
                        NavigationBarItem(
                            selected = destination?.hierarchy?.any { it.hasRoute(item.route::class) } == true,
                            onClick = { navController.navigateTab(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.title) },
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
            tabRoutes(navController, prefs, flags, tools, home)
            ledgerRoutes(navController, categories, categoriesLoading = !categoriesReady)
            productRoutes(navController, offline)
            accountRoutes(navController, prefs, appViewModel)
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
private fun NavGraphBuilder.tabRoutes(
    nav: NavHostController,
    prefs: AppPrefs,
    flags: HubFlags,
    tools: ToolsViewModel,
    home: HomeViewModel,
) {
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
            onSeeAll = { nav.navigate(Screen.TransactionList) },
            onQuickAction = { action ->
                val route = when (action) {
                    "send" -> Screen.Wallet
                    "upi" -> Screen.Scan
                    "offers" -> Screen.Promotions()
                    "accounts" -> Screen.Profile
                    else -> null
                }
                if (route != null) nav.navigate(route)
            },
            conflicts = conflicts.size,
            onConflicts = { nav.navigate(Screen.Conflicts) },
            viewModel = home,
        )
    }
    composable<Screen.PayHub> {
        PayHub(flags, { nav.navigate(Screen.Profile) }, nav::navigate)
    }
    composable<Screen.Passbook> {
        LedgerHistoryScreen(
            onBack = null,
            title = "Passbook",
            onOpenDetail = { nav.navigate(Screen.TransactionDetail(it)) },
        )
    }
    composable<Screen.Promotions>(deepLinks = promotionLinks()) {
        val offers: PromotionsViewModel = hiltViewModel()
        val shelves by offers.shelves.collectAsStateWithLifecycle()
        val selected by offers.selected.collectAsStateWithLifecycle()
        PromotionsHub(shelves, selected, offers::select) { nav.navigate(Screen.Profile) }
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
    composable<Screen.TransactionDetail>(deepLinks = appLinks<Screen.TransactionDetail>("transactions")) { backStackEntry ->
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
    composable<Screen.LedgerHistory> {
        LedgerHistoryScreen(
            onBack = back,
            onOpenDetail = { nav.navigate(Screen.TransactionDetail(it)) },
        )
    }
    composable<Screen.Charts> { ChartsScreen(onBack = back) }
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
private fun NavGraphBuilder.accountRoutes(nav: NavHostController, prefs: AppPrefs, app: AppViewModel) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<Screen.Profile> {
        ProfileScreen(
            prefs = prefs,
            showMockControls = BuildConfig.DEBUG && BuildConfig.FLAVOR == "mock",
            onBack = null,
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
        ScanScreen(liveCamera = true, onBack = back) { target ->
            nav.navigate(Screen.PayTo(target.vpa, target.amountMinor)) { popUpTo<Screen.Scan> { inclusive = true } }
        }
    }
    composable<Screen.PayTo>(deepLinks = appLinks<Screen.PayTo>("pay")) { entry ->
        val route = entry.toRoute<Screen.PayTo>()
        WalletScreen(onBack = back, debugStepUp = BuildConfig.DEBUG, prefillHandle = route.handle, prefillMinor = route.amountMinor)
    }
}

private data class BarItem(val title: String, val route: Screen, val icon: ImageVector)

/** Shows detail actions. */
@Composable
private fun DetailActions(nav: NavHostController, tx: Transaction) {
    OutlinedButton(onClick = { nav.navigate(Screen.Dispute(tx.id)) }, modifier = Modifier.fillMaxWidth()) {
        Text("Report a problem")
    }
}
