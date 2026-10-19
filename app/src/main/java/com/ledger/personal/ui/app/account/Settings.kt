package com.ledger.personal.ui.app.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.NumericKeypad
import com.ledger.personal.ui.feature.PinDots
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.security.SecureWindow
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.StatusChip
import com.ledger.personal.ui.theme.ThemeMode

/** Shows switch row. */
@Composable
fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ListRow(icon, title, Modifier.weight(1f), subtitle = subtitle)
        Switch(checked, onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}

/** Shows the settings screen. */
@Composable
fun SettingsScreen(prefs: AppPrefs, onBack: () -> Unit, onTheme: (ThemeMode) -> Unit, onHideBalances: (Boolean) -> Unit) {
    FeatureScaffold("Settings", onBack) {
        SectionCard(title = "Appearance") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = prefs.theme == mode,
                        onClick = { onTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) { Text(mode.label) }
                }
            }
        }
        SectionCard(title = "Privacy") {
            SwitchRow(
                Icons.Outlined.Visibility,
                "Hide balances",
                "Tap the eye icon to reveal for this session",
                prefs.hideBalances,
                onHideBalances,
            )
        }
        SectionCard(title = "Language") {
            ListRow(Icons.Outlined.Language, prefs.language, subtitle = "Chosen during setup")
        }
        Text("LedgerOne 1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shows the security screen. */
@Composable
fun SecurityScreen(
    prefs: AppPrefs,
    onBack: () -> Unit,
    onBiometric: (Boolean) -> Unit,
    onChangeMpin: (String) -> Unit,
    onSignOutEverywhere: () -> Unit,
    onBind: ((Result<String>) -> Unit) -> Unit,
) {
    var binding by rememberSaveable { mutableStateOf(false) }
    var bindError by rememberSaveable { mutableStateOf<String?>(null) }
    var changing by rememberSaveable { mutableStateOf(false) }
    var entry by rememberSaveable { mutableStateOf("") }
    var changed by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    SecureWindow()
    FeatureScaffold("Security centre", onBack) {
        StatusBanner(
            "MPIN set, device bound, app locks after 30 seconds in the background.",
            BannerTone.SUCCESS,
            title = "Your account is protected",
        )
        SectionCard(title = "Sign-in") {
            SwitchRow(Icons.Outlined.Fingerprint, "Biometric unlock", "Fingerprint or face instead of MPIN", prefs.biometric, onBiometric)
            ListRow(Icons.Outlined.Password, "Change MPIN", subtitle = "4 digits", onClick = { changing = !changing })
            if (changed) StatusBanner("MPIN updated.", BannerTone.SUCCESS)
            if (changing) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("Enter a new MPIN")
                    PinDots(entry.length, 4)
                }
                NumericKeypad(onDigit = { digit ->
                    entry += digit
                    if (entry.length == 4) {
                        onChangeMpin(entry)
                        entry = ""
                        changing = false
                        changed = true
                    }
                }, onDelete = { entry = entry.dropLast(1) })
            }
        }
        SectionCard(title = "Devices") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListRow(
                    Icons.Outlined.PhoneAndroid,
                    "This phone",
                    Modifier.weight(1f),
                    subtitle = if (prefs.deviceId.isBlank()) "Not bound to a hardware key" else "Hardware key · ${prefs.deviceId}",
                )
                if (prefs.deviceId.isBlank()) StatusChip("Not bound", BannerTone.WARNING) else StatusChip("Bound", BannerTone.SUCCESS)
            }
            bindError?.let { StatusBanner(it, BannerTone.ERROR, title = "Couldn't bind this phone") }
            ListRow(
                Icons.Outlined.Fingerprint,
                if (binding) "Binding…" else if (prefs.deviceId.isBlank()) "Bind this phone" else "Re-bind this phone",
                subtitle = "Creates a key only your biometrics can unlock",
                onClick = {
                    binding = true
                    bindError = null
                    onBind { result ->
                        binding = false
                        bindError = result.exceptionOrNull()?.message
                    }
                }.takeIf { !binding },
            )
            ListRow(
                Icons.Outlined.WarningAmber,
                "Sign out of all devices",
                subtitle = "Clears this device and asks for OTP again",
                iconTint = MaterialTheme.colorScheme.error,
                onClick = { confirmSignOut = true },
            )
        }
        SectionCard(title = "Tips") {
            ListRow(Icons.Outlined.Shield, "We never ask for your MPIN or OTP", subtitle = "Not on calls, SMS or email")
        }
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out everywhere?") },
            text = { Text("You'll need your phone number and an OTP to sign in again.") },
            confirmButton = { TextButton(onClick = onSignOutEverywhere) { Text("Sign out") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

/** Notice. */
private data class Notice(val title: String, val body: String, val time: String, val tone: BannerTone, val unread: Boolean)

private val notices = listOf(
    Notice("₹450.00 debited", "Swiggy · Savings •• 4821", "10:42 AM", BannerTone.INFO, true),
    Notice("Salary credited", "+₹85,000.00 to Salary •• 1093", "Yesterday", BannerTone.SUCCESS, true),
    Notice("New device sign-in blocked", "Someone tried to sign in from a Pixel 8", "Mon", BannerTone.ERROR, false),
    Notice("Electricity bill due", "₹500.00 due on 12 Oct", "Sun", BannerTone.WARNING, false),
    Notice("USD/INR crossed 83.50", "Your FX alert fired", "Sat", BannerTone.INFO, false),
)

/** Shows the notifications screen. */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    FeatureScaffold("Notifications", onBack) {
        notices.forEach { notice ->
            SectionCard {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusChip(if (notice.unread) "New" else notice.time, notice.tone)
                    if (notice.unread) Text(notice.time, style = MaterialTheme.typography.bodySmall)
                }
                Text(notice.title, fontWeight = FontWeight.SemiBold)
                Text(notice.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        ListRow(Icons.Outlined.Notifications, "Older notifications are kept for 90 days")
    }
}

private val faqs = listOf(
    "How do I raise a dispute?" to "Call or email us below with the transaction reference. We respond within 3 working days.",
    "Why is a new payee on hold?" to "New beneficiaries cool for 30 minutes before the first payment, to stop fraud.",
    "I forgot my MPIN" to "On the unlock screen tap Forgot MPIN, verify your phone with an OTP and set a new one.",
    "Is my data shared?" to "Only with your consent, and only to process payments you start.",
)

/** Shows the help screen. */
@Composable
fun HelpScreen(onBack: () -> Unit) {
    var open by rememberSaveable { mutableStateOf<Int?>(null) }
    FeatureScaffold("Help and support", onBack) {
        SectionCard(title = "Frequently asked") {
            faqs.forEachIndexed { index, (question, answer) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ListRow(Icons.Outlined.Shield, question, Modifier.weight(1f), onClick = { open = if (open == index) null else index })
                    Icon(if (open == index) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                }
                if (open == index) Text(answer, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SectionCard(title = "Contact us") {
            ListRow(Icons.Outlined.Call, "1800 123 4567", subtitle = "24×7, toll-free")
            ListRow(Icons.Outlined.Email, "care@ledgerone.in", subtitle = "Replies within a day")
        }
    }
}

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    PersonalLedgerTheme { SettingsScreen(AppPrefs(name = "Asha Rao"), onBack = {}, onTheme = {}, onHideBalances = {}) }
}

@PreviewLightDark
@Composable
private fun SecurityPreview() {
    PersonalLedgerTheme {
        SecurityScreen(AppPrefs(name = "Asha Rao"), onBack = {}, onBiometric = {}, onChangeMpin = {}, onSignOutEverywhere = {}, onBind = {})
    }
}

@PreviewLightDark
@Composable
private fun NotificationsPreview() {
    PersonalLedgerTheme { NotificationsScreen(onBack = {}) }
}

@PreviewLightDark
@Composable
private fun HelpPreview() {
    PersonalLedgerTheme { HelpScreen(onBack = {}) }
}
