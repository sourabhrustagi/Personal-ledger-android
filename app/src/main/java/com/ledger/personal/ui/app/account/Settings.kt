package com.ledger.personal.ui.app.account

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.SectionCard
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

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    PersonalLedgerTheme { SettingsScreen(AppPrefs(name = "Asha Rao"), onBack = {}, onTheme = {}, onHideBalances = {}) }
}
