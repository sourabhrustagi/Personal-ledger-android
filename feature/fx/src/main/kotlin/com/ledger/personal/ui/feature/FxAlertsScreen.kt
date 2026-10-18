package com.ledger.personal.ui.feature

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal

/** Shows the fx alerts screen. */
@Composable
fun FxAlertsScreen(onBack: () -> Unit, viewModel: FxAlertsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermission(granted)
    }
    LaunchedEffect(Unit) {
        viewModel.onPermission(notificationsAllowed(context))
        viewModel.startWatching()
        FxAlertSchedule.enqueuePeriodic(context)
    }
    LaunchedEffect(state.firedCount) {
        if (state.firedCount > 0) FxAlertSchedule.enqueueNow(context)
    }
    var targetText by rememberSaveable { mutableStateOf(BigDecimal.valueOf(state.thresholdMinor, 2).toPlainString()) }

    FeatureScaffold("Rate alerts", onBack, subtitle = "Get notified when a rate crosses your target") {
        HeroCard(
            label = "${state.pair} · live",
            value = state.rateMinor?.let { formatMoney(it) } ?: "—",
            supporting = state.rateMinor?.let { rate ->
                val gap = state.thresholdMinor - rate
                if (gap > 0) "${formatMoney(gap)} below your target" else "At or above your target"
            } ?: "Waiting for the first quote…",
        )
        state.errorMessage?.let { StatusBanner(it, BannerTone.ERROR) }
        SectionCard(title = "Alert me when USD/INR reaches") {
            AmountField(targetText, { targetText = it }, label = "Target rate")
            PrimaryButton(
                "Save alert",
                onClick = { parseMinor(targetText)?.let(viewModel::setThreshold) },
                enabled = parseMinor(targetText)?.let { it != state.thresholdMinor } == true,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.AutoMirrored.Outlined.TrendingUp, size = 36)
                Column(Modifier.weight(1f)) {
                    Text("Target ${formatMoney(state.thresholdMinor)}", fontWeight = FontWeight.Medium)
                    Text(
                        if (state.triggered) "Crossed · we sent you a notification" else "Active · checks every quote",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                StatusChip(if (state.triggered) "Triggered" else "Active", if (state.triggered) BannerTone.SUCCESS else BannerTone.INFO)
            }
        }
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconBadge(Icons.Outlined.NotificationsActive, size = 36)
                Column(Modifier.weight(1f)) {
                    Text("Push notifications", fontWeight = FontWeight.Medium)
                    Text("Alerts while the app is closed", style = MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = !state.permissionDenied,
                    onCheckedChange = { on ->
                        if (!on) {
                            viewModel.onPermission(false)
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.onPermission(notificationsAllowed(context))
                        }
                    },
                )
            }
            if (state.permissionDenied) {
                StatusBanner("Notifications are off. Alerts will only show on this screen.", BannerTone.WARNING)
            }
            DetailRow("Alerts sent", state.firedCount.toString())
        }
    }
}

/** Notifications allowed. */
private fun notificationsAllowed(context: Context): Boolean {
    val runtime = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    return runtime && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun FxAlertsPreview() {
    PersonalLedgerTheme { FxAlertsScreen(onBack = {}, viewModel = FxAlertsViewModel()) }
}

@PreviewLightDark
@Composable
private fun FxAlertsDeniedPreview() {
    val vm = remember {
        FxAlertsViewModel().apply {
            onPermission(false)
            check(8_200L)
        }
    }
    PersonalLedgerTheme { FxAlertsScreen(onBack = {}, viewModel = vm) }
}

@PreviewLightDark
@Composable
private fun FxAlertsTriggeredPreview() {
    val vm = remember {
        FxAlertsViewModel().apply {
            setThreshold(8_000L)
            check(8_400L)
        }
    }
    PersonalLedgerTheme { FxAlertsScreen(onBack = {}, viewModel = vm) }
}
