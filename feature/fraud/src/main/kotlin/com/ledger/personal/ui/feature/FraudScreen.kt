package com.ledger.personal.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Switch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.GppMaybe
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledger.personal.mock.SeedFraudAlert

/** Shows the fraud screen. */
@Composable
fun FraudScreen(onBack: () -> Unit, role: String = "ANALYST", viewModel: FraudViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var shadow by rememberSaveable { mutableStateOf(false) }
    var shadowLog by rememberSaveable { mutableStateOf(listOf<String>()) }
    LaunchedEffect(role) { viewModel.openAs(role) }
    LaunchedEffect(state.pendingId) {
        val id = state.pendingId ?: return@LaunchedEffect
        val result = snackbar.showSnackbar("Card will be blocked", actionLabel = "Undo", duration = SnackbarDuration.Long)
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undo(id)
            FraudCommitSchedule.cancel(context, id)
        } else {
            viewModel.commit()
        }
    }
    FeatureScaffold("Fraud review", onBack, subtitle = "Ranked by risk score", snackbarHost = { SnackbarHost(snackbar) }) {
        if (state.errorMessage != null) {
            EmptyState(Icons.Outlined.Lock, "Analysts only", "You need the analyst role to review flagged transactions.")
            return@FeatureScaffold
        }
        HeroCard(
            label = "Needs review",
            value = "${state.open.size} of ${state.alerts.size} flagged",
            supporting = "${state.blocked} card(s) blocked this session",
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Live · updated just now", BannerTone.SUCCESS)
            Text("Shadow mode", Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
            Switch(shadow, { shadow = it }, modifier = Modifier.semantics { contentDescription = "Shadow mode" })
        }
        if (shadow) {
            StatusBanner(
                "Decisions are logged for model evaluation and not enforced. Cards stay active.",
                BannerTone.WARNING,
                title = "Shadow mode on",
            )
        }
        RiskScale()
        if (state.open.isEmpty()) {
            EmptyState(Icons.Outlined.VerifiedUser, "All clear", "No flagged transactions are waiting for review.")
        }
        state.alerts.sortedByDescending { it.score }.forEach { alert ->
            AlertCard(
                alert = alert,
                blocked = alert.id in state.blockedIds,
                cleared = alert.id in state.clearedIds,
                pending = alert.id == state.pendingId,
                shadow = shadow,
                shadowLogged = alert.id in shadowLog,
                onBlock = {
                    if (shadow) {
                        shadowLog = shadowLog + alert.id
                    } else {
                        viewModel.schedule(alert.id)
                        FraudCommitSchedule.enqueue(context, alert.id)
                    }
                },
                onClear = { viewModel.clear(alert.id) },
            )
        }
        AuditLog(state.alerts, state.blockedIds, state.clearedIds, shadowLog)
    }
}

/** Severity. */
private fun severity(score: Int): Pair<String, BannerTone> = when {
    score >= HIGH_RISK -> "High risk" to BannerTone.ERROR
    score >= MEDIUM_RISK -> "Medium risk" to BannerTone.WARNING
    else -> "Low risk" to BannerTone.INFO
}

private const val HIGH_RISK = 80
private const val MEDIUM_RISK = 50

/** Shows risk scale. */
@Composable
private fun RiskScale() {
    SectionCard(title = "Risk score") {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("0–49 Low" to BannerTone.INFO, "50–79 Medium" to BannerTone.WARNING, "80–100 High" to BannerTone.ERROR).forEach { (label, tone) ->
                Column(Modifier.weight(1f)) {
                    androidx.compose.foundation.layout.Box(
                        Modifier.fillMaxWidth().height(6.dp).background(toneColor(tone), RoundedCornerShape(3.dp)),
                    )
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Shows audit log. */
@Composable
private fun AuditLog(alerts: List<SeedFraudAlert>, blocked: Set<String>, cleared: Set<String>, shadowLog: List<String>) {
    val rows = alerts.mapNotNull { alert ->
        when (alert.id) {
            in blocked -> "Blocked card for ${alert.merchant}"
            in cleared -> "Marked ${alert.merchant} genuine"
            in shadowLog -> "Shadow: would block ${alert.merchant}"
            else -> null
        }
    }
    SectionCard(title = "Audit trail") {
        if (rows.isEmpty()) Text("No decisions yet this session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        rows.forEach { Text("• $it · by you", style = MaterialTheme.typography.bodyMedium) }
    }
}

/** Shows alert card. */
@Composable
private fun AlertCard(
    alert: SeedFraudAlert,
    blocked: Boolean,
    cleared: Boolean,
    pending: Boolean,
    shadow: Boolean,
    shadowLogged: Boolean,
    onBlock: () -> Unit,
    onClear: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(Icons.Outlined.GppMaybe, tint = if (blocked || cleared) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error)
            Column(Modifier.weight(1f)) {
                Text(alert.merchant, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text("${alert.location} · ${alert.time}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatMoney(alert.amountMinor), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        val (level, tone) = severity(alert.score)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("$level · ${alert.score}", tone)
            StatusChip(alert.reason, BannerTone.INFO)
            when {
                blocked -> StatusChip("Card blocked", BannerTone.ERROR)
                cleared -> StatusChip("Marked genuine", BannerTone.SUCCESS)
                pending -> StatusChip("Blocking…", BannerTone.INFO)
                shadowLogged -> StatusChip("Logged (shadow)", BannerTone.WARNING)
            }
        }
        if (!blocked && !cleared && !pending && !shadowLogged) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Genuine")
                }
                Button(
                    onClick = onBlock,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Block, contentDescription = null)
                    Text(if (shadow) " Log block" else " Block card")
                }
            }
        }
    }
}
