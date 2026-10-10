package com.ledger.personal.ui.app

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** True while the active network reports internet. */
@Composable
fun rememberConnected(): Boolean {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(ConnectivityManager::class.java) }
    var connected by remember { mutableStateOf(hasInternet(manager)) }
    DisposableEffect(manager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { connected = hasInternet(manager) }
            override fun onLost(network: Network) { connected = hasInternet(manager) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                connected = hasInternet(manager)
            }
        }
        manager.registerDefaultNetworkCallback(callback)
        onDispose { manager.unregisterNetworkCallback(callback) }
    }
    return connected
}

/** True when the active network reports internet. */
private fun hasInternet(manager: ConnectivityManager): Boolean {
    val network = manager.activeNetwork ?: return false
    val caps = manager.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
