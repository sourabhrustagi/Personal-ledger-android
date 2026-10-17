package com.ledger.personal.ui.feature

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Allowed values of bank sheet. */
enum class BankSheet { UPI, PAYEE }

/** Shows banking sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankingSheet(sheet: BankSheet, state: BankingUiState, viewModel: BankingViewModel, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = {
        viewModel.dismissMessage()
        onDismiss()
    }) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (sheet) {
                BankSheet.UPI -> UpiRequest(state.upi, viewModel::showUpi)
                BankSheet.PAYEE -> AddPayee(state, viewModel)
            }
        }
    }
}

/** Shows upi request. */
@Composable
private fun UpiRequest(payload: String?, onCreate: (String, Long) -> Unit) {
    val context = LocalContext.current
    var vpa by rememberSaveable { mutableStateOf("you@okaxis") }
    var amount by rememberSaveable { mutableStateOf("") }
    SheetTitle("Request money", "Share a UPI link that any UPI app can pay.")
    OutlinedTextField(vpa, { vpa = it.trim() }, label = { Text("Your UPI ID") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
    AmountField(amount, { amount = it })
    PrimaryButton("Create request", onClick = { parseMinor(amount)?.let { onCreate(vpa, it) } }, enabled = vpa.contains('@') && parseMinor(amount) != null)
    payload?.let { link ->
        SectionCard {
            Text("Payment link", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(link, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            OutlinedButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link)
                    context.startActivity(Intent.createChooser(send, "Share payment link"))
                },
                shape = RoundedCornerShape(12.dp),
            ) { Text("Share") }
        }
    }
}

/** Shows add payee. */
@Composable
private fun AddPayee(state: BankingUiState, viewModel: BankingViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var account by rememberSaveable { mutableStateOf("") }
    SheetTitle("Add payee", "For your security, new payees can receive money 30 minutes after you add them.")
    if (state.payee == null) {
        OutlinedTextField(name, { name = it }, label = { Text("Payee name") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(
            account,
            { account = it.trim() },
            label = { Text("Account number or UPI ID") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(
            "Add payee",
            onClick = { viewModel.addBeneficiary(System.currentTimeMillis(), name.trim()) },
            enabled = name.isNotBlank() && account.length >= 6,
        )
    } else {
        StatusBanner("${state.payee} was added.", BannerTone.SUCCESS)
        if (state.cooling) {
            StatusBanner("You can send money to ${state.payee} 30 minutes after adding them.", BannerTone.WARNING, title = "Payee is cooling")
        }
        PrimaryButton("Pay ${state.payee}", onClick = { viewModel.pay(System.currentTimeMillis()) })
    }
}

/** Shows sheet title. */
@Composable
private fun SheetTitle(title: String, message: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@PreviewLightDark
@Composable
private fun UpiRequestPreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            UpiRequest("upi://pay?pa=you@okaxis&am=500.00", onCreate = { _, _ -> })
        }
    }
}

@PreviewLightDark
@Composable
private fun AddPayeePreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AddPayee(BankingUiState(), BankingViewModel())
        }
    }
}

@PreviewLightDark
@Composable
private fun AddPayeeCoolingPreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            AddPayee(BankingUiState(payee = "Asha Rao", cooling = true), BankingViewModel())
        }
    }
}
