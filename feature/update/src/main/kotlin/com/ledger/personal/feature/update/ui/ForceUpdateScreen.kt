@file:Suppress("MatchingDeclarationName")

package com.ledger.personal.feature.update.ui

import com.ledger.personal.feature.update.util.PlayStore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.components.BannerTone
import com.ledger.personal.ui.components.IconBadge
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.components.StatusBanner

/** UI state for the force update screen. */
data class ForceUpdateUiState(val versionName: String = "")

/** Blocks the app until this version is updated from the store. */
@Composable
fun ForceUpdateScreen(uiState: ForceUpdateUiState) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier
                    .safeDrawingPadding()
                    .padding(24.dp)
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(48.dp))
                IconBadge(Icons.Outlined.SystemUpdate, size = 72)
                Text("Update required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Version ${uiState.versionName} is no longer supported. Update LedgerOne to keep your account secure and keep paying.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusBanner("Your money and data are safe. Nothing is lost by updating.", BannerTone.INFO)
                PrimaryButton(
                    "Update now",
                    onClick = { PlayStore.openStore(context) },
                    icon = Icons.Outlined.SystemUpdate,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Blocks the app until this version is updated from the store. */
@Composable
fun ForceUpdateScreen(versionName: String) {
    ForceUpdateScreen(ForceUpdateUiState(versionName))
}

/** A newer build the person can install now or keep using this one. */
@Composable
fun SoftUpdateDialog(onUpdate: () -> Unit, onLater: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
        title = { Text("Update available") },
        text = { Text("A newer version of LedgerOne is ready. You can update now, or keep using this one.") },
        confirmButton = {
            TextButton(onClick = {
                PlayStore.openStore(context)
                onUpdate()
            }) { Text("Update") }
        },
        dismissButton = { TextButton(onClick = onLater) { Text("Not now") } },
    )
}

@PreviewLightDark
@Composable
private fun ForceUpdatePreview() {
    PersonalLedgerTheme { ForceUpdateScreen("2.0.0") }
}

@PreviewLightDark
@Composable
private fun SoftUpdatePreview() {
    PersonalLedgerTheme { SoftUpdateDialog(onUpdate = {}, onLater = {}) }
}
