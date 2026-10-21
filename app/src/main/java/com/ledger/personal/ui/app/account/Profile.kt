package com.ledger.personal.ui.app.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.navigation.Screen
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows the accounts and profile screen. */
@Composable
fun ProfileScreen(
    prefs: AppPrefs,
    showMockControls: Boolean,
    onBack: (() -> Unit)? = null,
    onOpen: (Screen) -> Unit,
    onLock: () -> Unit,
    onLogOut: () -> Unit,
) {
    var confirmLogOut by rememberSaveable { mutableStateOf(false) }
    val open: (Screen) -> () -> Unit = { target -> { onOpen(target) } }
    FeatureScaffold("Accounts", onBack) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                IconBadge(Icons.Outlined.Person, size = 56)
                Column(Modifier.weight(1f)) {
                    Text(
                        prefs.name.ifBlank { "LedgerOne member" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text("+91 ${prefs.phone.ifBlank { "—" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (showMockControls) {
            SectionCard(title = "Account") {
                ListRow(Icons.Outlined.Settings, "Settings", subtitle = "Theme, privacy, language", onClick = open(Screen.Settings))
                ListRow(Icons.Outlined.BugReport, "Mock controls", subtitle = "Debug builds only", onClick = open(Screen.MockControls))
            }
        } else {
            SectionCard(title = "Account") {
                ListRow(Icons.Outlined.Settings, "Settings", subtitle = "Theme, privacy, language", onClick = open(Screen.Settings))
            }
        }
        ListRow(
            Icons.AutoMirrored.Outlined.Logout,
            "Lock app",
            subtitle = "Ask for OTP next time",
            onClick = onLock,
        )
        ListRow(
            Icons.AutoMirrored.Outlined.Logout,
            "Log out",
            subtitle = "Ends this sign-in. Saved transactions stay on this phone",
            iconTint = MaterialTheme.colorScheme.error,
            onClick = { confirmLogOut = true },
        )
    }
    if (confirmLogOut) {
        AlertDialog(
            onDismissRequest = { confirmLogOut = false },
            title = { Text("Log out?") },
            text = {
                Text(
                    "You'll need your phone number and OTP to open the app again. " +
                        "Transactions saved on this phone stay here, including ones still waiting to sync.",
                )
            },
            confirmButton = { TextButton(onClick = onLogOut) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { confirmLogOut = false }) { Text("Cancel") } },
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfilePreview() {
    PersonalLedgerTheme {
        ProfileScreen(AppPrefs(name = "Asha Rao"), showMockControls = true, onBack = null, onOpen = {}, onLock = {}, onLogOut = {})
    }
}
