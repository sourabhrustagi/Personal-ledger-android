package com.ledger.personal.ui.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
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
import androidx.core.util.Consumer
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
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.ui.app.AppViewModel
import com.ledger.personal.ui.app.DemoViewModel
import com.ledger.personal.ui.app.HubFlags
import com.ledger.personal.ui.app.offlineNow
import com.ledger.personal.ui.app.rememberConnected
import com.ledger.personal.ui.app.hubs.PayHub
import com.ledger.personal.ui.app.hubs.PromotionsHub
import com.ledger.personal.ui.app.hubs.PromotionsViewModel
import com.ledger.personal.ui.app.scan.ScanScreen
import com.ledger.personal.ui.feature.LedgerHistoryScreen
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
    val home: HomeViewModel = hiltViewModel()
    val homeState by home.uiState.collectAsStateWithLifecycle()
    fun routeFor(id: String): Screen? = when (id) {
        "home" -> Screen.Home
        "pay" -> Screen.PayHub
        "passbook" -> Screen.Passbook
        "offers" -> Screen.Promotions()
        else -> null
    }
    fun iconFor(id: String) = when (id) {
        "home" -> Icons.Outlined.Home
        "pay" -> Icons.Outlined.QrCodeScanner
        "passbook" -> Icons.AutoMirrored.Outlined.ReceiptLong
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
            tabRoutes(navController, prefs, flags, home)
            ledgerRoutes(navController, categories, categoriesLoading = !categoriesReady)
            productRoutes(navController, offline)
            payRoutes(navController)
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
    home: HomeViewModel,
) {
    composable<Screen.Home> {
        HomeScreen(
            name = prefs.name,
            balanceMinor = if (flags.empty) 0L else MockCatalog.bankAccounts.first().balanceMinor,
            revealBalance = flags.reveal,
            offline = flags.offline,
            onToggleReveal = flags.onToggleReveal,
            viewModel = home,
        )
    }
    composable<Screen.PayHub> {
        PayHub(flags, nav::navigate)
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
        val uiState by offers.uiState.collectAsStateWithLifecycle()
        PromotionsHub(uiState, offers::select)
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
            onNavigateEdit = null,
            onRetry = { viewModel.loadTransaction(route.transactionId) },
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
    composable<Screen.Wallet> { WalletScreen(onBack = back, debugStepUp = BuildConfig.DEBUG, offline = offline) }
}

/** Pay & transfer routes. */
private fun NavGraphBuilder.payRoutes(
    nav: NavHostController,
) {
    val back: () -> Unit = { nav.popBackStack() }
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
