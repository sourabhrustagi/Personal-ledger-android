package com.ledger.personal.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.data.tools.Dispute
import com.ledger.personal.data.tools.DisputeReason
import com.ledger.personal.domain.sync.SyncConflict
import com.ledger.personal.data.tools.conflictFields
import com.ledger.personal.data.tools.stage
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.DetailRow
import com.ledger.personal.ui.feature.EmptyState
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.StatusChip
import com.ledger.personal.ui.feature.Timeline
import com.ledger.personal.ui.feature.TimelineStep
import com.ledger.personal.ui.feature.formatMoney

/** Shows the dispute screen. */
@Composable
fun DisputeScreen(transactionId: String, tools: ToolsViewModel, onBack: () -> Unit, onTrack: () -> Unit) {
    val all by tools.transactions.collectAsStateWithLifecycle()
    val filed by tools.disputeDesk.disputes.collectAsStateWithLifecycle()
    val tx = all.firstOrNull { it.id == transactionId }
    var reason by rememberSaveable { mutableStateOf<DisputeReason?>(null) }
    var details by rememberSaveable { mutableStateOf("") }
    val existing = filed.firstOrNull { it.transactionId == transactionId }
    FeatureScaffold("Report a problem", onBack) {
        if (tx == null) {
            EmptyState(Icons.Outlined.Gavel, "Transaction not found", "It may still be syncing. Try again in a moment.")
            return@FeatureScaffold
        }
        SectionCard {
            DetailRow("Transaction", tx.description)
            DetailRow("Amount", formatMoney(tx.amountInCents, tx.currency), emphasize = true)
        }
        if (existing != null) {
            DisputeStatus(existing)
            PrimaryButton("See all disputes", onClick = onTrack, modifier = Modifier.fillMaxWidth())
            return@FeatureScaffold
        }
        SectionCard(title = "What went wrong?") {
            DisputeReason.entries.forEach { option ->
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(reason == option, onClick = { reason = option }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(reason == option, onClick = null)
                    Text(option.label, Modifier.padding(start = 8.dp))
                }
            }
        }
        OutlinedTextField(
            details,
            { if (it.length <= 500) details = it },
            label = { Text("Tell us more") },
            supportingText = { Text("${details.length}/500 · at least 10 characters") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )
        if (reason == DisputeReason.UNAUTHORISED) {
            StatusBanner(
                "Freeze your card from Money → Cards right away so no more payments go through.",
                BannerTone.WARNING,
                title = "Protect your card",
            )
        }
        PrimaryButton(
            "Submit dispute",
            onClick = { reason?.let { tools.fileDispute(tx, it, details) } },
            enabled = reason != null && details.trim().length >= 10,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Shows dispute status. */
@Composable
private fun DisputeStatus(dispute: Dispute) {
    val stage = dispute.stage(System.currentTimeMillis())
    SectionCard(title = "Dispute ${dispute.reference}") {
        StatusChip(if (stage == 0) "Submitted" else "Under review", BannerTone.WARNING)
        Text(dispute.reason.label, fontWeight = FontWeight.Medium)
        Timeline(
            listOf(
                TimelineStep("Submitted", "We've logged your dispute", true),
                TimelineStep("Under review", "A specialist checks it with the merchant", stage >= 1),
                TimelineStep("Resolved", "Usually within 3 working days", false),
            ),
        )
    }
}

/** Shows the disputes screen. */
@Composable
fun DisputesScreen(tools: ToolsViewModel, onBack: () -> Unit) {
    val filed by tools.disputeDesk.disputes.collectAsStateWithLifecycle()
    FeatureScaffold("Your disputes", onBack) {
        if (filed.isEmpty()) {
            EmptyState(Icons.Outlined.Gavel, "No disputes", "Open a transaction and tap Report a problem to raise one.")
        }
        filed.forEach { DisputeStatus(it) }
    }
}

/** Shows the conflict screen. */
@Composable
fun ConflictScreen(tools: ToolsViewModel, onBack: () -> Unit) {
    val conflicts by tools.inbox.conflicts.collectAsStateWithLifecycle()
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    FeatureScaffold("Sync conflicts", onBack, subtitle = "Changed on this phone and elsewhere") {
        error?.let { StatusBanner(it, BannerTone.ERROR) }
        if (conflicts.isEmpty()) {
            EmptyState(
                Icons.Outlined.SyncProblem,
                "Everything is in sync",
                "Conflicts appear here when the same transaction changes in two places.",
            )
        }
        conflicts.forEach { conflict ->
            ConflictCard(conflict) { keepMine ->
                tools.resolve(conflict, keepMine) { result -> error = result.exceptionOrNull()?.message }
            }
        }
    }
}

/** Shows conflict card. */
@Composable
private fun ConflictCard(conflict: SyncConflict, onResolve: (Map<String, Boolean>) -> Unit) {
    val diffs = remember(conflict) { conflictFields(conflict.local, conflict.server) }
    val keepMine = remember(conflict) { mutableStateMapOf<String, Boolean>() }
    SectionCard(title = conflict.local.description) {
        diffs.forEach { diff ->
            Text(diff.field, style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(true to "This phone: ${diff.mine}", false to "Server: ${diff.theirs}").forEachIndexed { index, (mine, label) ->
                    SegmentedButton(
                        selected = keepMine[diff.field] == mine,
                        onClick = { keepMine[diff.field] = mine },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                    ) { Text(label, maxLines = 2) }
                }
            }
        }
        PrimaryButton(
            "Save merged version",
            onClick = { onResolve(keepMine.toMap()) },
            enabled = diffs.all { it.field in keepMine },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
