package com.ledger.personal.ui.app

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.LinkOff
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.SignalCellularConnectedNoInternet4Bar
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.StatusBanner

/** Shows the mock controls screen. */
@Composable
fun MockControlsScreen(onBack: () -> Unit, onSimulateConflict: () -> Unit) {
    val slow by MockSwitches.slow.collectAsStateWithLifecycle()
    val failures by MockSwitches.failures.collectAsStateWithLifecycle()
    val offline by MockSwitches.offline.collectAsStateWithLifecycle()
    val empty by MockSwitches.empty.collectAsStateWithLifecycle()
    val dropLive by MockSwitches.dropLive.collectAsStateWithLifecycle()
    val flakyPayments by MockSwitches.flakyPayments.collectAsStateWithLifecycle()
    FeatureScaffold("Mock controls", onBack, subtitle = "Debug build only") {
        StatusBanner("These switches change what the mock backend returns. The next request or refresh uses them.", BannerTone.INFO)
        SectionCard(title = "Network") {
            SwitchRow(Icons.Outlined.Speed, "Slow network", "Adds 2.5 s to every request", slow) { MockSwitches.slow.value = it }
            SwitchRow(Icons.Outlined.ErrorOutline, "Fail requests", "Every request returns a server error", failures) {
                MockSwitches.failures.value = it
            }
            SwitchRow(Icons.Outlined.CloudOff, "Offline", "No connection; payments are paused", offline) { MockSwitches.offline.value = it }
            SwitchRow(Icons.Outlined.LinkOff, "Drop live connection", "Ticker falls back to polling", dropLive) {
                MockSwitches.dropLive.value = it
            }
            SwitchRow(
                Icons.Outlined.SignalCellularConnectedNoInternet4Bar,
                "Flaky payments",
                "About 1 in 3 writes drop, are cancelled, or the server refuses",
                flakyPayments,
            ) { MockSwitches.flakyPayments.value = it }
        }
        SectionCard(title = "Data") {
            SwitchRow(Icons.Outlined.Inbox, "Empty data", "Accounts and sync return nothing", empty) { MockSwitches.empty.value = it }
            ListRow(
                Icons.Outlined.SyncProblem,
                "Create a sync conflict",
                subtitle = "Server edits your newest transaction",
                onClick = onSimulateConflict,
            )
            ListRow(Icons.Outlined.RestartAlt, "Reset all switches", onClick = MockSwitches::reset)
        }
    }
}
