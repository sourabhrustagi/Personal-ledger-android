package com.ledger.personal.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.mock.SeedAccount

/** Shows the banking screen. */
@Composable
fun BankingScreen(onBack: () -> Unit, onPayBills: () -> Unit, viewModel: BankingViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (!state.signedIn) {
        MpinLock(onBack, state.errorMessage, viewModel::signIn)
        return
    }
    var sheet by rememberSaveable { mutableStateOf<BankSheet?>(null) }
    FeatureScaffold(
        "Banking",
        onBack,
        subtitle = "Good to see you",
        actions = {
            IconButton(onClick = viewModel::signOut) { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sign out") }
        },
    ) {
        HeroCard(
            label = "Total balance",
            value = formatMoney(state.bankAccounts.sumOf { it.balanceMinor }),
            supporting = "${state.bankAccounts.size} accounts",
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.bankAccounts) { account -> AccountTile(account) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAction(Icons.Outlined.QrCode2, "Request", Modifier.weight(1f)) { sheet = BankSheet.UPI }
            QuickAction(Icons.AutoMirrored.Outlined.ReceiptLong, "Pay bills", Modifier.weight(1f), onPayBills)
            QuickAction(Icons.Outlined.PersonAdd, "Add payee", Modifier.weight(1f)) { sheet = BankSheet.PAYEE }
        }
        LoanCard(state)
    }
    sheet?.let { current ->
        BankingSheet(current, state, viewModel, onDismiss = { sheet = null })
    }
}

/** Shows account tile. */
@Composable
private fun AccountTile(account: SeedAccount) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
        Column(Modifier.width(180.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            IconBadge(Icons.Outlined.AccountBalance, size = 36)
            Text("${account.name} •• ${account.lastFour}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMoney(account.balanceMinor), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Shows quick action. */
@Composable
private fun QuickAction(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.height(84.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Shows loan card. */
@Composable
private fun LoanCard(state: BankingUiState) {
    SectionCard(title = "Personal loan") {
        DetailRow("Monthly EMI", formatMoney(state.emiMinor), emphasize = true)
        DetailRow("Principal", formatMoney(state.legs.sumOf { it.principalMinor }))
        DetailRow("Interest rate", "${LOAN_BPS / 100}% p.a. · $LOAN_MONTHS months")
        DetailRow("Total interest", formatMoney(state.legs.sumOf { it.interestMinor }))
        HorizontalDivider()
        Text("Next payments", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.legs.take(3).forEachIndexed { index, leg ->
            DetailRow("Month ${index + 1}", "${formatMoney(leg.principalMinor)} principal + ${formatMoney(leg.interestMinor)} interest")
        }
    }
}

/** Shows mpin lock. */
@Composable
private fun MpinLock(onBack: () -> Unit, error: String?, onSubmit: (String) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(pin) {
        if (pin.length == 4) {
            onSubmit(pin)
            pin = ""
        }
    }
    FeatureScaffold("Banking", onBack) {
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(Icons.Outlined.Lock, size = 72)
            Text("Enter your MPIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Your 4-digit banking PIN", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(4) { index ->
                    val filled = index < pin.length
                    Box(
                        Modifier.size(16.dp).clip(CircleShape).background(
                            if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    )
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        listOf("123", "456", "789", " 0<").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    Box(Modifier.weight(1f).height(64.dp), contentAlignment = Alignment.Center) {
                        when (key) {
                            ' ' -> Unit
                            '<' -> TextButton(onClick = { pin = pin.dropLast(1) }) { Text("Delete") }
                            else -> FilledTonalButton(
                                onClick = { if (pin.length < 4) pin += key },
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp),
                            ) { Text(key.toString(), style = MaterialTheme.typography.headlineSmall) }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun BankingLockPreview() {
    PersonalLedgerTheme { BankingScreen(onBack = {}, onPayBills = {}, viewModel = BankingViewModel()) }
}

@PreviewLightDark
@Composable
private fun BankingPreview() {
    val signedIn = remember { BankingViewModel().also { it.signIn("1234") } }
    PersonalLedgerTheme { BankingScreen(onBack = {}, onPayBills = {}, viewModel = signedIn) }
}
