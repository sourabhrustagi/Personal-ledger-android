package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.ledger.personal.ui.security.SecureWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.core.emiMinor
import com.ledger.personal.core.tenureFor
import com.ledger.personal.mock.MockCatalog
import kotlin.math.pow
import kotlin.math.roundToLong

private const val CREDIT_LIMIT_MINOR = 30_000_000L
private const val CREDIT_USED_MINOR = 8_245_000L

/** Shows the cards screen. */
@Composable
fun CardsScreen(onBack: () -> Unit) {
    var frozen by rememberSaveable { mutableStateOf(false) }
    var online by rememberSaveable { mutableStateOf(true) }
    var international by rememberSaveable { mutableStateOf(false) }
    var contactless by rememberSaveable { mutableStateOf(true) }
    var limit by rememberSaveable { mutableFloatStateOf(50_000f) }
    SecureWindow()
    FeatureScaffold("Cards", onBack, subtitle = "Debit and credit", actions = { ProtectedBadge(Modifier.padding(end = 12.dp)) }) {
        HeroCard(
            label = "Platinum debit · ${mask("4821")}",
            value = if (frozen) "Frozen" else "Active",
            supporting = "Linked to Savings ${mask("4821")} · valid till 08/29",
        )
        if (frozen) StatusBanner("All payments on this card are blocked until you unfreeze it.", BannerTone.WARNING, title = "Card frozen")
        SectionCard(title = "Controls") {
            ToggleRow(Icons.Outlined.AcUnit, "Freeze card", "Temporarily block every transaction", frozen) { frozen = it }
            ToggleRow(Icons.Outlined.CreditCard, "Online payments", "E-commerce and in-app", online && !frozen) { online = it }
            ToggleRow(Icons.Outlined.CreditCard, "International use", "Payments outside India", international && !frozen) { international = it }
            ToggleRow(Icons.Outlined.CreditCard, "Tap to pay", "Contactless up to ₹5,000", contactless && !frozen) { contactless = it }
        }
        SectionCard(title = "Daily spend limit") {
            Text(formatMoney(limit.toLong() * 100), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Slider(limit, { limit = it }, valueRange = 5_000f..2_00_000f, steps = 38, enabled = !frozen)
        }
        SectionCard(title = "Credit card · ${mask("9034")}") {
            DetailRow("Credit limit", formatMoney(CREDIT_LIMIT_MINOR))
            DetailRow("Outstanding", formatMoney(CREDIT_USED_MINOR), emphasize = true)
            DetailRow("Available", formatMoney(CREDIT_LIMIT_MINOR - CREDIT_USED_MINOR))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { CREDIT_USED_MINOR.toFloat() / CREDIT_LIMIT_MINOR },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).semantics { contentDescription = "27 percent of limit used" },
            )
            StatusBanner("Minimum due ₹4,122.50 by 18 Oct. Pay in full to avoid interest.", BannerTone.WARNING, title = "Bill generated")
        }
    }
}

/** Shows toggle row. */
@Composable
private fun ToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ListRow(icon, title, Modifier.weight(1f), subtitle = subtitle)
        Switch(on, onChange, modifier = Modifier.semantics { contentDescription = title })
    }
}

/** Allowed values of prepay mode. */
enum class PrepayMode(val label: String) { TENURE("Reduce tenure"), EMI("Reduce EMI") }

/** Shows the loans screen. */
@Composable
fun LoansScreen(onBack: () -> Unit) {
    var amountLakh by rememberSaveable { mutableFloatStateOf(5f) }
    var ratePct by rememberSaveable { mutableFloatStateOf(12f) }
    var months by rememberSaveable { mutableIntStateOf(LOAN_MONTHS) }
    var prepayText by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(PrepayMode.TENURE) }
    val principal = (amountLakh * 1_00_000).roundToLong() * 100
    val bps = (ratePct * 100).roundToLong().toInt()
    val emi = emiMinor(principal, bps, months)
    FeatureScaffold("Loans", onBack, subtitle = "Personal loan · ${mask("5512")}") {
        HeroCard(
            label = "Outstanding principal",
            value = formatMoney(MockCatalog.loanPrincipalMinor),
            supporting = "Next EMI ${formatMoney(emiMinor(MockCatalog.loanPrincipalMinor, LOAN_BPS, LOAN_MONTHS))} on 5 Nov · 12% p.a.",
        )
        SectionCard(title = "EMI calculator") {
            LabeledSlider("Loan amount", "₹${"%.1f".format(amountLakh)} lakh", amountLakh, 1f..50f) { amountLakh = it }
            LabeledSlider("Interest rate", "${"%.1f".format(ratePct)}% p.a.", ratePct, 8f..18f) { ratePct = it }
            LabeledSlider("Tenure", "$months months", months.toFloat(), 12f..84f) { months = it.roundToLong().toInt() }
            DetailRow("Monthly EMI", formatMoney(emi), emphasize = true)
            DetailRow("Total interest", formatMoney(emi * months - principal))
            DetailRow("Total payable", formatMoney(emi * months))
        }
        PrepaySection(prepayText, { prepayText = it }, mode, { mode = it })
    }
}

/** Shows prepay section. */
@Composable
private fun PrepaySection(text: String, onText: (String) -> Unit, mode: PrepayMode, onMode: (PrepayMode) -> Unit) {
    val outstanding = MockCatalog.loanPrincipalMinor
    val emi = emiMinor(outstanding, LOAN_BPS, LOAN_MONTHS)
    val prepay = parseMinor(text)
    SectionCard(title = "Part-prepay") {
        AmountField(text, onText, label = "Prepayment amount", supportingText = "No charges on part-prepayment")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            PrepayMode.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = option == mode,
                    onClick = { onMode(option) },
                    shape = SegmentedButtonDefaults.itemShape(index, PrepayMode.entries.size),
                ) { Text(option.label) }
            }
        }
        when {
            prepay == null -> Text("Enter an amount to compare options.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            prepay >= outstanding -> StatusBanner("That clears the loan. Use foreclosure from the branch or app.", BannerTone.INFO)
            mode == PrepayMode.TENURE -> {
                val months = tenureFor(outstanding - prepay, LOAN_BPS, emi)
                DetailRow("New tenure", "$months months", emphasize = true)
                DetailRow("You save", "${LOAN_MONTHS - months} EMIs")
            }
            else -> {
                val newEmi = emiMinor(outstanding - prepay, LOAN_BPS, LOAN_MONTHS)
                DetailRow("New EMI", formatMoney(newEmi), emphasize = true)
                DetailRow("Lower by", "${formatMoney(emi - newEmi)} / month")
            }
        }
    }
}

/** Shows labeled slider. */
@Composable
private fun LabeledSlider(label: String, value: String, current: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
        Slider(current, onChange, valueRange = range, modifier = Modifier.semantics { contentDescription = "$label, $value" })
    }
}

private const val FD_RATE = 0.0725

// ponytail: quarterly compounding in Double, rounded to paise; good enough for a preview, the bank quotes the binding figure.
/** Fd maturity minor. */
fun fdMaturityMinor(principalMinor: Long, months: Int, annualRate: Double = FD_RATE): Long =
    (principalMinor * (1 + annualRate / 4).pow(months / 3.0)).roundToLong()

/** Shows the deposits screen. */
@Composable
fun DepositsScreen(onBack: () -> Unit) {
    val fd = MockCatalog.bankAccounts.last()
    var text by rememberSaveable { mutableStateOf("") }
    var months by rememberSaveable { mutableIntStateOf(12) }
    var booked by rememberSaveable { mutableStateOf<String?>(null) }
    val amount = parseMinor(text)
    FeatureScaffold("Deposits", onBack, subtitle = "Fixed deposits at 7.25% p.a.") {
        SectionCard(title = "Your deposits") {
            ListRow(Icons.Outlined.Savings, "${fd.name} ${mask(fd.lastFour)}", subtitle = "Matures 14 Mar 2027 · auto-renew on", trailing = formatMoney(fd.balanceMinor))
            DetailRow("Maturity value", formatMoney(fdMaturityMinor(fd.balanceMinor, 12)))
        }
        booked?.let { StatusBanner(it, BannerTone.SUCCESS, title = "Deposit booked") }
        SectionCard(title = "Open a new FD") {
            AmountField(text, { text = it }, label = "Deposit amount", supportingText = "Minimum ₹5,000")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(6, 12, 24, 36).forEach { option ->
                    androidx.compose.material3.FilterChip(option == months, { months = option }, label = { Text("$option mo") })
                }
            }
            if (amount != null) {
                DetailRow("Interest earned", formatMoney(fdMaturityMinor(amount, months) - amount))
                DetailRow("Maturity value", formatMoney(fdMaturityMinor(amount, months)), emphasize = true)
            }
            PrimaryButton(
                "Book deposit",
                onClick = { booked = "${formatMoney(amount ?: 0)} for $months months from Savings ${mask("4821")}." },
                enabled = amount != null && amount >= 5_00_000,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun CardsPreview() {
    PersonalLedgerTheme { CardsScreen(onBack = {}) }
}

@PreviewLightDark
@Composable
private fun LoansPreview() {
    PersonalLedgerTheme { LoansScreen(onBack = {}) }
}

@PreviewLightDark
@Composable
private fun DepositsPreview() {
    PersonalLedgerTheme { DepositsScreen(onBack = {}) }
}
