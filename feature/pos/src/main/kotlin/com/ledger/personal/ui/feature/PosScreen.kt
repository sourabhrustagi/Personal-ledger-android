package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.core.PayEvent
import com.ledger.personal.core.PayState
import kotlinx.coroutines.delay

@Composable
fun PosScreen(onBack: () -> Unit, viewModel: PosViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refreshPending() }
    var waitingForCard by rememberSaveable(state.sale) { mutableStateOf(false) }
    var tender by rememberSaveable(state.sale) { mutableStateOf<Tender?>(null) }
    val completeOffCard = {
        viewModel.onEvent(PayEvent.TAP)
        viewModel.onEvent(PayEvent.APPROVE)
        viewModel.onEvent(PayEvent.SETTLE)
    }
    LaunchedEffect(waitingForCard) {
        if (waitingForCard && state.pay == PayState.IDLE) {
            delay(CARD_WAIT_MS)
            viewModel.onEvent(PayEvent.TAP)
            waitingForCard = false
        }
    }
    FeatureScaffold(
        "Accept payment",
        onBack,
        subtitle = "Terminal · ${state.pendingCount} awaiting settlement",
    ) {
        HardwareStatus()
        state.errorMessage?.let { StatusBanner(it, BannerTone.ERROR) }
        when {
            waitingForCard -> PresentCard(state.amountMinor) {
                waitingForCard = false
                tender = null
            }
            state.pay == PayState.IDLE && tender == Tender.UPI -> UpiTender(state.amountMinor, completeOffCard) { tender = null }
            state.pay == PayState.IDLE && tender == Tender.CASH -> CashTender(state.amountMinor, completeOffCard) { tender = null }
            state.pay == PayState.IDLE && tender == Tender.CHOOSING -> TenderPicker(state.amountMinor) { choice ->
                tender = choice
                if (choice == Tender.CARD) waitingForCard = true
            }
            state.pay == PayState.IDLE -> {
                Keypad(state.amountMinor, viewModel) { tender = Tender.CHOOSING }
                SettlementCard(state, viewModel::settle)
            }
            state.pay == PayState.READ -> CardRead(state) { viewModel.onEvent(PayEvent.APPROVE) }
            state.pay == PayState.AUTHORIZED -> Approved(state) { viewModel.onEvent(PayEvent.SETTLE) }
            else -> Receipt(state, tender ?: Tender.CARD, viewModel::refund, viewModel::newSale)
        }
    }
}

@Composable
private fun Keypad(amountMinor: Long, viewModel: PosViewModel, onCharge: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Amount", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(amountMinor), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
    }
    listOf("123", "456", "789", "0⌫").forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { key ->
                FilledTonalButton(
                    onClick = { if (key == '⌫') viewModel.onBackspace() else viewModel.onDigit(key) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(if (key == '0') 2f else 1f).height(64.dp),
                ) {
                    if (key == '⌫') {
                        Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete")
                    } else {
                        Text(key.toString(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        }
    }
    PrimaryButton(
        if (amountMinor > 0) "Charge ${formatMoney(amountMinor)}" else "Enter an amount",
        onClick = onCharge,
        enabled = amountMinor > 0,
        icon = Icons.Outlined.PointOfSale,
    )
}

@Composable
private fun PresentCard(amountMinor: Long, onCancel: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconBadge(Icons.Outlined.Contactless, size = 120)
        Text(formatMoney(amountMinor), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tap, insert or swipe card", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(Modifier.fillMaxWidth(0.6f))
    }
    OutlinedButton(onClick = onCancel, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
}

@Composable
private fun CardRead(state: PosUiState, onApprove: () -> Unit) {
    PaymentCard(state.lastFour)
    SectionCard {
        DetailRow("Amount", formatMoney(state.amountMinor), emphasize = true)
        DetailRow("Method", "Contactless")
    }
    PrimaryButton("Authorize payment", onClick = onApprove)
}

@Composable
private fun Approved(state: PosUiState, onSettle: () -> Unit) {
    StatusBanner("The card issuer approved ${formatMoney(state.amountMinor)}.", BannerTone.SUCCESS, title = "Approved")
    PaymentCard(state.lastFour)
    PrimaryButton("Print receipt and finish", onClick = onSettle)
}

@Composable
private fun Receipt(state: PosUiState, tender: Tender, onRefund: () -> Unit, onNewSale: () -> Unit) {
    var confirmRefund by rememberSaveable(state.sale) { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBadge(Icons.Outlined.CheckCircle, size = 88)
        Text("Payment complete", style = MaterialTheme.typography.titleMedium)
        Text(formatMoney(state.amountMinor), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    }
    SectionCard(title = "Receipt") {
        DetailRow("Paid by", if (tender == Tender.CARD) "Card •••• ${state.lastFour ?: "----"}" else tender.label)
        DetailRow("Reference", state.reference)
        HorizontalDivider()
        DetailRow("Total", formatMoney(state.amountMinor), emphasize = true)
    }
    if (state.refunded) {
        StatusBanner("${formatMoney(state.amountMinor)} returned to the customer's ${tender.label.lowercase()}.", BannerTone.SUCCESS, title = "Refunded")
    }
    StatusBanner("Settlement runs at the end of the day. ${state.pendingCount} entries awaiting settlement.", BannerTone.INFO)
    PrimaryButton("New sale", onClick = onNewSale)
    if (!state.refunded) {
        OutlinedButton(onClick = { confirmRefund = true }, modifier = Modifier.fillMaxWidth()) { Text("Refund this sale") }
    }
    if (confirmRefund) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmRefund = false },
            title = { Text("Refund ${formatMoney(state.amountMinor)}?") },
            text = { Text("The full amount goes back to the customer's ${tender.label.lowercase()}. This can't be undone.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    confirmRefund = false
                    onRefund()
                }) { Text("Refund") }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmRefund = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun SettlementCard(state: PosUiState, onSettle: () -> Unit) {
    SectionCard(title = "End of day") {
        DetailRow("Entries awaiting settlement", state.pendingCount.toString())
        DetailRow("Net amount", formatMoney(state.pendingTotalMinor), emphasize = true)
        state.settlement?.let { StatusBanner(it, BannerTone.SUCCESS, title = "Batch closed") }
        OutlinedButton(onClick = onSettle, enabled = state.pendingCount > 0, modifier = Modifier.fillMaxWidth()) { Text("Settle now") }
    }
}

@Composable
private fun PaymentCard(lastFour: String?) {
    HeroCard(label = "Visa · Contactless", value = "•••• •••• •••• ${lastFour ?: "····"}", modifier = Modifier.aspectRatio(1.9f)) {
        Icon(Icons.Outlined.CreditCard, contentDescription = null)
    }
}

private const val CARD_WAIT_MS = 1_500L

enum class Tender(val label: String) { CHOOSING("Choose"), CARD("Card"), UPI("UPI"), CASH("Cash") }

@Composable
private fun HardwareStatus() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusChip("Card reader connected", BannerTone.SUCCESS)
        StatusChip("Printer ready", BannerTone.SUCCESS)
    }
}

@Composable
private fun TenderPicker(amountMinor: Long, onPick: (Tender) -> Unit) {
    Text("How is the customer paying?", style = MaterialTheme.typography.titleMedium)
    Text(formatMoney(amountMinor), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    SectionCard {
        ListRow(Icons.Outlined.Contactless, "Card", subtitle = "Tap, insert or swipe", onClick = { onPick(Tender.CARD) })
        ListRow(Icons.Outlined.QrCode2, "UPI", subtitle = "Customer scans a QR code", onClick = { onPick(Tender.UPI) })
        ListRow(Icons.Outlined.Payments, "Cash", subtitle = "Calculate change due", onClick = { onPick(Tender.CASH) })
    }
}

@Composable
private fun UpiTender(amountMinor: Long, onReceived: () -> Unit, onCancel: () -> Unit) {
    SectionCard(title = "Scan to pay") {
        IconBadge(Icons.Outlined.QrCode2, size = 160)
        Text(com.ledger.personal.core.upiPayload("store@ledgerone", amountMinor), style = MaterialTheme.typography.bodySmall)
        Text("Waiting for the customer's UPI app to confirm…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    PrimaryButton("Payment received", onClick = onReceived)
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Choose another method") }
}

@Composable
private fun CashTender(amountMinor: Long, onDone: () -> Unit, onCancel: () -> Unit) {
    var given by rememberSaveable { mutableStateOf("") }
    val givenMinor = parseMinor(given)
    SectionCard(title = "Cash") {
        DetailRow("Amount due", formatMoney(amountMinor), emphasize = true)
        AmountField(given, { given = it }, label = "Cash received")
        if (givenMinor != null && givenMinor >= amountMinor) DetailRow("Change to return", formatMoney(givenMinor - amountMinor), emphasize = true)
        if (givenMinor != null && givenMinor < amountMinor) StatusBanner("${formatMoney(amountMinor - givenMinor)} still due.", BannerTone.WARNING)
    }
    PrimaryButton("Complete sale", onClick = onDone, enabled = givenMinor != null && givenMinor >= amountMinor, icon = Icons.Outlined.Print)
    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Choose another method") }
}
