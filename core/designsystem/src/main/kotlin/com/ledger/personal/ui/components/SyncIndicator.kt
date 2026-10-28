package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Allowed values of sync state. */
enum class SyncState(val label: String, val tone: BannerTone, val icon: ImageVector) {
    SYNCED("Synced", BannerTone.SUCCESS, Icons.Outlined.CloudDone),
    SYNCING("Syncing", BannerTone.INFO, Icons.Outlined.Sync),
    WAITING("Waiting for network", BannerTone.WARNING, Icons.Outlined.CloudOff),
    FAILED("Couldn't reach the bank", BannerTone.ERROR, Icons.Outlined.ErrorOutline),
}

/** Shows sync indicator. */
@Composable
fun SyncIndicator(state: SyncState, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(state.icon, contentDescription = null, tint = state.tone.toneColor(), modifier = Modifier.size(16.dp))
        Text(state.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@PreviewLightDark
@Composable
private fun SyncIndicatorPreview() {
    PersonalLedgerTheme {
        SyncIndicator(SyncState.SYNCING)
    }
}
