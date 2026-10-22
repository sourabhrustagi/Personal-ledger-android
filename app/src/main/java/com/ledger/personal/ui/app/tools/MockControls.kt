@file:Suppress("MatchingDeclarationName")

package com.ledger.personal.ui.app.tools

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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.ui.app.account.SwitchRow
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.StatusBanner

/** UI state for the mock controls screen. */
data class MockControlsUiState(
    val slow: Boolean = false,
    val failures: Boolean = false,
    val offline: Boolean = false,
    val empty: Boolean = false,
    val dropLive: Boolean = false,
    val flakyPayments: Boolean = false,
)

/** Shows the mock controls screen using MockSwitches state. */
@Composable
fun MockControlsScreen(onBack: () -> Unit, onSimulateConflict: () -> Unit) {
    val slow by MockSwitches.slow.collectAsStateWithLifecycle()
    val failures by MockSwitches.failures.collectAsStateWithLifecycle()
    val offline by MockSwitches.offline.collectAsStateWithLifecycle()
    val empty by MockSwitches.empty.collectAsStateWithLifecycle()
    val dropLive by MockSwitches.dropLive.collectAsStateWithLifecycle()
    val flakyPayments by MockSwitches.flakyPayments.collectAsStateWithLifecycle()

    val uiState = MockControlsUiState(
        slow = slow,
        failures = failures,
        offline = offline,
        empty = empty,
        dropLive = dropLive,
        flakyPayments = flakyPayments,
    )

    MockControlsScreen(
        uiState = uiState,
        onBack = onBack,
        onSimulateConflict = onSimulateConflict,
        onSlowChange = { MockSwitches.slow.value = it },
        onFailuresChange = { MockSwitches.failures.value = it },
        onOfflineChange = { MockSwitches.offline.value = it },
        onDropLiveChange = { MockSwitches.dropLive.value = it },
        onFlakyPaymentsChange = { MockSwitches.flakyPayments.value = it },
        onEmptyChange = { MockSwitches.empty.value = it },
        onReset = MockSwitches::reset,
    )
}

/** Shows the mock controls screen with MockControlsUiState. */
@Composable
fun MockControlsScreen(
    uiState: MockControlsUiState,
    onBack: () -> Unit,
    onSimulateConflict: () -> Unit,
    onSlowChange: (Boolean) -> Unit,
    onFailuresChange: (Boolean) -> Unit,
    onOfflineChange: (Boolean) -> Unit,
    onDropLiveChange: (Boolean) -> Unit,
    onFlakyPaymentsChange: (Boolean) -> Unit,
    onEmptyChange: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    FeatureScaffold("Mock controls", onBack, subtitle = "Debug build only") {
        StatusBanner("These switches change what the mock backend returns. The next request or refresh uses them.", BannerTone.INFO)
        SectionCard(title = "Network") {
            SwitchRow(Icons.Outlined.Speed, "Slow network", "Adds 2.5 s to every request", uiState.slow, onSlowChange)
            SwitchRow(
                Icons.Outlined.ErrorOutline,
                "Fail requests",
                "Every request returns a server error",
                uiState.failures,
                onFailuresChange,
            )
            SwitchRow(Icons.Outlined.CloudOff, "Offline", "No connection; payments are paused", uiState.offline, onOfflineChange)
            SwitchRow(Icons.Outlined.LinkOff, "Drop live connection", "Ticker falls back to polling", uiState.dropLive, onDropLiveChange)
            SwitchRow(
                Icons.Outlined.SignalCellularConnectedNoInternet4Bar,
                "Flaky payments",
                "About 1 in 3 writes drop, are cancelled, or the server refuses",
                uiState.flakyPayments,
                onFlakyPaymentsChange,
            )
        }
        SectionCard(title = "Data") {
            SwitchRow(Icons.Outlined.Inbox, "Empty data", "Accounts and sync return nothing", uiState.empty, onEmptyChange)
            ListRow(
                Icons.Outlined.SyncProblem,
                "Create a sync conflict",
                subtitle = "Server edits your newest transaction",
                onClick = onSimulateConflict,
            )
            ListRow(Icons.Outlined.RestartAlt, "Reset all switches", onClick = onReset)
        }
    }
}

@PreviewLightDark
@Composable
private fun MockControlsPreview() {
    PersonalLedgerTheme { MockControlsScreen(onBack = {}, onSimulateConflict = {}) }
}
