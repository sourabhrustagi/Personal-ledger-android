package com.ledger.personal.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.GppGood
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.StatusChip
import com.ledger.personal.ui.navigation.Screen

private val modeRole = mapOf(AppMode.PERSONAL to "CUSTOMER", AppMode.POS to "MERCHANT", AppMode.FRAUD to "ANALYST")

/** Shows the profile screen. */
@Composable
fun ProfileScreen(
    prefs: AppPrefs,
    session: SessionState,
    roles: Set<String>,
    showMockControls: Boolean,
    onBack: () -> Unit,
    onMode: (AppMode) -> Unit,
    onOpen: (Screen) -> Unit,
    onLock: () -> Unit,
    onLogOut: () -> Unit,
) {
    var confirmLogOut by rememberSaveable { mutableStateOf(false) }
    val open: (Screen) -> () -> Unit = { target -> { onOpen(target) } }
    FeatureScaffold("Profile", onBack) {
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
                StatusChip("KYC pending", BannerTone.WARNING)
            }
        }
        SectionCard(title = "Mode") {
            AppMode.entries.filter { modeRole[it] in roles }.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth()
                        .selectable(selected = session.mode == mode, onClick = { onMode(mode) }, role = Role.RadioButton)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = session.mode == mode, onClick = null)
                    ListRow(
                        when (mode) {
                            AppMode.PERSONAL -> Icons.Outlined.Person
                            AppMode.POS -> Icons.Outlined.PointOfSale
                            AppMode.FRAUD -> Icons.Outlined.GppGood
                        },
                        mode.label,
                        Modifier.weight(1f),
                        subtitle = when (mode) {
                            AppMode.PERSONAL -> "Your money and payments"
                            AppMode.POS -> "Accept card, UPI and cash at the counter"
                            AppMode.FRAUD -> "Review flagged card activity (analysts)"
                        },
                    )
                }
            }
        }
        SectionCard(title = "Account") {
            ListRow(Icons.Outlined.VerifiedUser, "Complete KYC", subtitle = "Unlock higher limits", onClick = open(Screen.Kyc))
            ListRow(Icons.Outlined.Security, "Security centre", subtitle = "MPIN, biometrics, devices", onClick = open(Screen.Security))
            ListRow(Icons.Outlined.Notifications, "Notifications", subtitle = "Alerts and activity", onClick = open(Screen.Notifications))
            ListRow(Icons.Outlined.Settings, "Settings", subtitle = "Theme, privacy, language", onClick = open(Screen.Settings))
            ListRow(Icons.Outlined.Category, "Categories", subtitle = "Add, rename or remove", onClick = open(Screen.Categories))
            ListRow(Icons.Outlined.Gavel, "Your disputes", subtitle = "Track problems you reported", onClick = open(Screen.Disputes))
            ListRow(Icons.AutoMirrored.Outlined.HelpOutline, "Help and support", subtitle = "FAQs and contact", onClick = open(Screen.Help))
            if (showMockControls) {
                ListRow(Icons.Outlined.BugReport, "Mock controls", subtitle = "Debug builds only", onClick = open(Screen.MockControls))
            }
        }
        ListRow(
            Icons.AutoMirrored.Outlined.Logout,
            "Lock app",
            subtitle = "Ask for MPIN next time",
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
                    "You'll need your MPIN to open the app again. " +
                        "Transactions saved on this phone stay here, including ones still waiting to sync.",
                )
            },
            confirmButton = { TextButton(onClick = onLogOut) { Text("Log out") } },
            dismissButton = { TextButton(onClick = { confirmLogOut = false }) { Text("Cancel") } },
        )
    }
}
