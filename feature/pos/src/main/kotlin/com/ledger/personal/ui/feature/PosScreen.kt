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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.ledger.personal.ui.security.SecureWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.core.ElectricityBills
import com.ledger.personal.domain.pay.FetchedBill
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.mock.SeedBiller
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.mock.SeedCard

/** Pay a bill with a card and PIN. The PIN is sealed before it leaves the phone. */
@Composable
fun PosScreen(onBack: () -> Unit, viewModel: PosViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    when (state.step) {
        PosStep.BILLS -> BillList(state, onBack, viewModel::selectBill)
        PosStep.ACCOUNT -> BillAccount(state, viewModel::back, viewModel::onBillNumber, viewModel::fetchBill, viewModel::continueToPay)
        PosStep.CARD -> CardList(state, viewModel::back, viewModel::selectCard)
        PosStep.PIN -> PinPad(state, viewModel::back, viewModel::confirmPin)
    }
}

/** Electricity bill number. Fetch can miss; the miss is the banner. */
@Composable
private fun BillAccount(
    state: PosUiState,
    onBack: () -> Unit,
    onNumber: (String) -> Unit,
    onFetch: () -> Unit,
    onPay: () -> Unit,
) {
    val fetched = state.fetched
    FeatureScaffold("Electricity bill", onBack, subtitle = "BESCOM · bill number") {
        if (state.fetchFailed && state.errorMessage != null) {
            StatusBanner(state.errorMessage, BannerTone.ERROR, title = "Bill not fetched")
        }
        if (fetched == null) {
            OutlinedTextField(
                value = state.billNumber,
                onValueChange = onNumber,
                label = { Text("Bill number") },
                singleLine = true,
                enabled = !state.fetching,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            if (!state.fetchFailed) state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "On file in this demo: ${ElectricityBills.onFile.keys.sorted().joinToString()}. Any other number is not fetched.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PrimaryButton(
                text = if (state.fetching) "Fetching…" else "Fetch bill",
                onClick = onFetch,
                enabled = !state.fetching,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            DetailRow("Bill number", fetched.billNumber)
            DetailRow("Account", fetched.holder)
            DetailRow("Amount due", formatMoney(fetched.amountMinor), emphasize = true)
            PrimaryButton("Pay this bill", onPay, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Bills due this month. */
@Composable
private fun BillList(state: PosUiState, onBack: () -> Unit, onPay: (SeedBiller) -> Unit) {
    FeatureScaffold("Pay bills", onBack, subtitle = "Bills due this month") {
        state.errorMessage?.let { StatusBanner(it, BannerTone.ERROR) }
        state.bill?.let { bill ->
            val biller = state.billers.firstOrNull { it.id == bill.substringBefore(':') }
            val paidMinor = bill.substringAfter(':').toLongOrNull() ?: return@let
            StatusBanner(
                "Paid ${formatMoney(paidMinor)} to ${biller?.name ?: "biller"}.",
                BannerTone.SUCCESS,
            )
        }
        state.billers.forEach { biller ->
            val paid = biller.id in state.paidBillers
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                ListRow(
                    icon = if (paid) Icons.Outlined.CheckCircle else categoryIcon("utilities"),
                    title = biller.name,
                    subtitle = if (paid) "Paid" else "${biller.dueDate} · ${formatMoney(biller.dueMinor)}",
                    modifier = Modifier.weight(1f),
                )
                if (!paid) TextButton(onClick = { onPay(biller) }) { Text("Pay") }
            }
        }
    }
}

/** Card to charge. */
@Composable
private fun CardList(state: PosUiState, onBack: () -> Unit, onPick: (SeedCard) -> Unit) {
    val due = state.fetched?.amountMinor ?: state.biller?.dueMinor
    val name = state.biller?.name
    FeatureScaffold("Pay with card", onBack, subtitle = if (name != null && due != null) "$name · ${formatMoney(due)}" else null) {
        state.cards.forEach { card ->
            ListRow(
                Icons.Outlined.CreditCard,
                card.name,
                subtitle = "${card.network} •••• ${card.lastFour}",
                onClick = { onPick(card) },
            )
        }
    }
}

/** Four-digit PIN. Sealed on the next screen action. */
@Composable
private fun PinPad(state: PosUiState, onBack: () -> Unit, onPin: (String) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    LaunchedEffect(pin, state.paying) {
        if (pin.length == 4 && !state.paying) {
            onPin(pin)
            pin = ""
        }
    }
    SecureWindow()
    FeatureScaffold("Enter PIN", onBack, subtitle = state.card?.let { "${it.network} •••• ${it.lastFour}" }) {
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconBadge(Icons.Outlined.Lock, size = 72)
            Text("Card PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
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
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        listOf("123", "456", "789", " 0<").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { key ->
                    Box(Modifier.weight(1f).height(64.dp), contentAlignment = Alignment.Center) {
                        when (key) {
                            ' ' -> Unit
                            '<' -> TextButton(onClick = { pin = pin.dropLast(1) }) { Text("Delete") }
                            else -> FilledTonalButton(
                                onClick = { if (pin.length < 4 && !state.paying) pin += key },
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

@PreviewLightDark
@Composable
private fun BillListPreview() {
    PersonalLedgerTheme { BillList(PosUiState(), onBack = {}, onPay = {}) }
}

@PreviewLightDark
@Composable
private fun BillAccountPreview() {
    PersonalLedgerTheme {
        BillAccount(
            PosUiState(biller = MockCatalog.billers.first { it.id == "electricity" }),
            onBack = {},
            onNumber = {},
            onFetch = {},
            onPay = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun BillNotFetchedPreview() {
    PersonalLedgerTheme {
        BillAccount(
            PosUiState(
                billNumber = "999999",
                fetchFailed = true,
                errorMessage = "The electricity board did not return a bill for this number. " +
                    "Check the bill number and try again. Your card was not charged.",
            ),
            onBack = {},
            onNumber = {},
            onFetch = {},
            onPay = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun BillFetchedPreview() {
    PersonalLedgerTheme {
        BillAccount(
            PosUiState(fetched = FetchedBill("104829", 50_000L, "BESCOM")),
            onBack = {},
            onNumber = {},
            onFetch = {},
            onPay = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun CardListPreview() {
    PersonalLedgerTheme {
        CardList(
            PosUiState(
                biller = MockCatalog.billers.first { it.id == "electricity" },
                fetched = FetchedBill("104829", 50_000L, "BESCOM"),
            ),
            onBack = {},
            onPick = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PinPadPreview() {
    PersonalLedgerTheme {
        PinPad(PosUiState(card = MockCatalog.cards.first()), onBack = {}, onPin = {})
    }
}
