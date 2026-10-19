package com.ledger.personal.ui.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.theme.PersonalLedgerTheme

private val languages = listOf("English", "हिन्दी", "தமிழ்", "తెలుగు")

/** Shows welcome step with language selection. */
@Composable
fun WelcomeStep(language: String, onLanguage: (String) -> Unit, onStart: () -> Unit) {
    Spacer(Modifier.height(32.dp))
    IconBadge(Icons.Outlined.AccountBalance, size = 64)
    Text("LedgerOne", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
    Text(
        "Your money, payments and accounts in one secure place.",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("Choose language", style = MaterialTheme.typography.titleSmall)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        languages.forEach { option ->
            FilterChip(selected = option == language, onClick = { onLanguage(option) }, label = { Text(option) })
        }
    }
    StatusBanner("Bank-grade encryption. We never share your data without consent.", BannerTone.INFO, title = "Secure by design")
    PrimaryButton("Get started", onClick = onStart, modifier = Modifier.fillMaxWidth())
}

@PreviewLightDark
@Composable
private fun WelcomeStepPreview() {
    PersonalLedgerTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                WelcomeStep(language = "English", onLanguage = {}, onStart = {})
            }
        }
    }
}
