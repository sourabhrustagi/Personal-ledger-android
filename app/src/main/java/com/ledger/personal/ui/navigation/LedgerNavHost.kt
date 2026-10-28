package com.ledger.personal.ui.navigation

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.ledger.personal.ui.components.HubFlags
import com.ledger.personal.ui.app.offlineNow
import com.ledger.personal.ui.app.rememberConnected
import com.ledger.personal.feature.hubs.pay.PayHub
import com.ledger.personal.feature.hubs.offers.PromotionsHub
import com.ledger.personal.feature.hubs.offers.PromotionsViewModel
import com.ledger.personal.feature.scan.ui.ScanScreen
import com.ledger.personal.ui.components.OfflineScreen
import com.ledger.personal.feature.wallet.ui.WalletScreen
import com.ledger.personal.feature.ledger.history.LedgerHistoryScreen
import com.ledger.personal.feature.ledger.home.HomeScreen
import com.ledger.personal.feature.ledger.home.HomeViewModel
import com.ledger.personal.feature.ledger.list.TransactionListScreen
import com.ledger.personal.feature.ledger.list.TransactionListViewModel

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
    var retryCount by rememberSaveable { mutableIntStateOf(0) }
    val connected = rememberConnected(retryCount)
    val offline = offlineNow(mockOffline, connected)
    var offlineDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(offline) {
        if (!offline) {
            offlineDismissed = false
        }
    }

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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .consumeWindowInsets(inner),
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home,
                modifier = Modifier.fillMaxSize(),
            ) {
                tabRoutes(navController, prefs, flags, home)
                ledgerRoutes(navController)
                productRoutes(navController, offline)
                payRoutes(navController)
            }

            AnimatedVisibility(
                visible = offline && !offlineDismissed,
                enter = fadeIn() + slideInVertically { it / 6 },
                exit = fadeOut() + slideOutVertically { it / 6 },
            ) {
                OfflineScreen(
                    onRetry = { retryCount++ },
                    onContinueOffline = { offlineDismissed = true },
                )
            }
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
        PayHub(
            flags = flags,
            onScanClick = { nav.navigate(Screen.Scan) },
            onPayTo = { recipient, amount -> nav.navigate(Screen.PayTo(recipient, amount)) },
        )
    }
    composable<Screen.Passbook> {
        LedgerHistoryScreen(
            onBack = null,
            title = "Passbook",
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
            onProductsClick = { nav.popBackStack() },
            paging = viewModel.pages.collectAsLazyPagingItems(),
        )
    }
}

/** Ledger routes. */
private fun NavGraphBuilder.ledgerRoutes(nav: NavHostController) {
    listRoute(nav)
}

/** Product routes. */
private fun NavGraphBuilder.productRoutes(nav: NavHostController, offline: Boolean) {
    val back: () -> Unit = { nav.popBackStack() }
    composable<Screen.LedgerHistory> {
        LedgerHistoryScreen(onBack = back)
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
