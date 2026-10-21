package com.ledger.personal.ui.app.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RequestQuote
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VerifiedUser
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
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.StatusChip
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
                StatusChip("KYC pending", BannerTone.WARNING)
            }
        }
        SectionCard(title = "Banking and products") {
            ListRow(Icons.Outlined.AccountBalance, "Accounts", subtitle = "Statements, UPI and balances", onClick = open(Screen.Banking))
            ListRow(Icons.Outlined.CreditCard, "Cards", subtitle = "Freeze, limits and controls", onClick = open(Screen.Cards))
            ListRow(Icons.Outlined.RequestQuote, "Loans", subtitle = "EMI calculator and prepay", onClick = open(Screen.Loans))
            ListRow(Icons.Outlined.Savings, "Deposits", subtitle = "Fixed deposits at 7.25% p.a.", onClick = open(Screen.Deposits))
            ListRow(Icons.Outlined.History, "All transactions", subtitle = "Full paged history", onClick = open(Screen.LedgerHistory))
            ListRow(Icons.Outlined.Description, "Statements", subtitle = "Monthly PDF for Savings", onClick = open(Screen.Statements))
        }
        SectionCard(title = "Account") {
            ListRow(Icons.Outlined.VerifiedUser, "Complete KYC", subtitle = "Unlock higher limits", onClick = open(Screen.Kyc))
            ListRow(Icons.Outlined.Security, "Security centre", subtitle = "OTP, biometrics, devices", onClick = open(Screen.Security))
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
