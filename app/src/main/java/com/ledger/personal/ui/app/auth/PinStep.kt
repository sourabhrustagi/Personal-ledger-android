package com.ledger.personal.ui.app.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.NumericKeypad
import com.ledger.personal.ui.feature.PinDots
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.theme.PersonalLedgerTheme

private const val MPIN_LENGTH = 4

/** Shows MPIN creation and confirmation step. */
@Composable
fun PinStep(
    title: String,
    message: String,
    expected: String? = null,
    onMismatch: () -> Unit = {},
    onDone: (String) -> Unit,
) {
    var entry by rememberSaveable(title) { mutableStateOf("") }
    var mismatch by rememberSaveable(title) { mutableStateOf(false) }
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        PinDots(entry.length, MPIN_LENGTH, Modifier.padding(vertical = 24.dp), error = mismatch)
    }
    if (mismatch) StatusBanner("MPINs didn't match. Create it again.", BannerTone.ERROR)
    if (expected == null) Text("Avoid 1234, 0000 or your birth year.", style = MaterialTheme.typography.bodySmall)
    NumericKeypad(
        onDigit = { digit ->
            if (entry.length < MPIN_LENGTH) entry += digit
            if (entry.length == MPIN_LENGTH) {
                when {
                    expected == null || entry == expected -> onDone(entry)
                    else -> {
                        mismatch = true
                        entry = ""
                        onMismatch()
                    }
                }
            }
        },
        onDelete = { entry = entry.dropLast(1) },
    )
}

@PreviewLightDark
@Composable
private fun PinStepPreview() {
    PersonalLedgerTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                PinStep("Create your 4-digit MPIN", "You'll use it to open LedgerOne and approve payments.") {}
            }
        }
    }
}
