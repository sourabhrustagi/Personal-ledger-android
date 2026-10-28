package com.ledger.personal.feature.wallet.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.ledger.personal.feature.wallet.payment.PaymentRail
import com.ledger.personal.core.formatApiError
import android.os.Build
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.ledger.personal.ui.security.SecureWindow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.components.AmountField
import com.ledger.personal.ui.components.BannerTone
import com.ledger.personal.ui.components.DetailRow
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.HeroCard
import com.ledger.personal.ui.components.IconBadge
import com.ledger.personal.ui.components.ListRow
import com.ledger.personal.ui.components.NumericKeypad
import com.ledger.personal.ui.components.PaymentsOfflineNotice
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.components.ProtectedBadge
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.components.StatusBanner
import com.ledger.personal.ui.components.formatMoney
import com.ledger.personal.ui.components.parseMinor
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.personal.core.TRANSACTION_ATTEMPTS
import com.ledger.personal.mock.MockCatalog
import java.math.BigDecimal

/** Shows the wallet screen. */
@Composable
fun WalletScreen(
    onBack: () -> Unit,
    debugStepUp: Boolean = false,
    offline: Boolean = false,
    prefillHandle: String? = null,
    prefillMinor: Long = 0L,
    viewModel: WalletViewModel = hiltViewModel(),
) {
    LaunchedEffect(prefillHandle, prefillMinor) {
        prefillHandle?.let(viewModel::onRecipient)
        if (prefillMinor > 0) viewModel.onAmount(prefillMinor)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SecureWindow()
    FeatureScaffold(state.step.title, onBack, subtitle = "UPI · Instant", actions = { ProtectedBadge(Modifier.padding(end = 12.dp)) }) {
        if (offline && state.step != TransferStep.RECEIPT) PaymentsOfflineNotice()
        when (state.step) {
            TransferStep.FORM -> TransferForm(state, viewModel, offline)
            TransferStep.REVIEW -> if (offline) TransferForm(state, viewModel, true) else TransferReview(state, debugStepUp, viewModel)
            TransferStep.RECEIPT -> TransferReceipt(state, onDone = onBack, viewModel = viewModel)
        }
    }
}

/** Shows transfer form. */
@Composable
private fun TransferForm(state: WalletUiState, viewModel: WalletViewModel, offline: Boolean) {
    val context = LocalContext.current
    val contactPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                try {
                    context.contentResolver.query(
                        contactUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ),
                        null,
                        null,
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val numCol = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val number = if (numCol >= 0) cursor.getString(numCol) else null
                            val clean = number?.replace(Regex("[^0-9+]"), "")
                            if (!clean.isNullOrBlank()) {
                                viewModel.onRecipient(clean)
                            }
                        }
                    }
                } catch (_: SecurityException) {
                    // Ignore gracefully
                }
            }
        }
    }

    var amountText by rememberSaveable {
        mutableStateOf(if (state.amountMinor > 0) BigDecimal.valueOf(state.amountMinor, 2).stripTrailingZeros().toPlainString() else "")
    }
    HeroCard(label = "Available balance", value = formatMoney(state.balanceMinor), supporting = "Savings •• 4821")
    SectionCard(title = "Send to") {
        OutlinedTextField(
            value = state.recipient,
            onValueChange = viewModel::onRecipient,
            label = { Text(stringResource(AppStrings.LABEL_RECIPIENT)) },
            placeholder = { Text(stringResource(AppStrings.LABEL_RECIPIENT_PLACEHOLDER)) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            trailingIcon = {
                IconButton(onClick = {
                    viewModel.onPickContactClicked {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPicker.launch(intent)
                    }
                }) {
                    Icon(Icons.Outlined.Contacts, contentDescription = "Select from contacts")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = {
                viewModel.onPickContactClicked {
                    val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                    contactPicker.launch(intent)
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(AppStrings.LABEL_SELECT_FROM_CONTACTS))
        }
        Text(
            stringResource(AppStrings.SECTION_SAVED_CONTACTS),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MockCatalog.contacts.forEach { contact ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ListRow(
                    icon = Icons.Outlined.Person,
                    title = contact.name,
                    subtitle = contact.handle,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.onSelectContactClicked(contact.handle) },
                )
                RadioButton(selected = state.recipient == contact.handle, onClick = { viewModel.onSelectContactClicked(contact.handle) })
            }
        }
    }
    SectionCard(title = stringResource(AppStrings.LABEL_AMOUNT)) {
        AmountField(
            text = amountText,
            onTextChange = {
                amountText = it
                viewModel.onAmount(parseMinor(it) ?: 0L)
            },
            isError = state.status == TransferStatus.INSUFFICIENT,
            supportingText = if (state.status == TransferStatus.INSUFFICIENT) "More than your available balance" else null,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(100L, 500L, 1_000L).forEach { rupees ->
                AssistChip(
                    onClick = {
                        amountText = rupees.toString()
                        viewModel.onPresetAmountClicked(rupees)
                    },
                    label = { Text("₹$rupees") },
                )
            }
        }
        OutlinedTextField(
            value = state.note,
            onValueChange = viewModel::onNote,
            label = { Text(stringResource(AppStrings.LABEL_NOTE_OPTIONAL)) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        )
    }
    state.errorMessage?.takeIf { state.status != TransferStatus.INSUFFICIENT }?.let {
        StatusBanner(formatApiError(it), BannerTone.ERROR, title = "Validation Error")
    }
    val continueLabel = stringResource(AppStrings.ACTION_CONTINUE)
    PrimaryButton(
        text = if (state.amountMinor > 0) "$continueLabel · ${formatMoney(state.amountMinor)}" else continueLabel,
        onClick = viewModel::onContinueClicked,
        enabled = !offline && state.recipient.isNotBlank() && state.amountMinor > 0,
    )
}

/** Shows transfer review. */
@Composable
private fun TransferReview(state: WalletUiState, debugStepUp: Boolean, viewModel: WalletViewModel) {
    val contact = MockCatalog.contacts.firstOrNull { it.handle == state.recipient }
    SectionCard {
        Text(stringResource(AppStrings.LABEL_YOU_ARE_SENDING), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(state.amountMinor), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        HorizontalDivider()
        DetailRow("To", contact?.name ?: state.recipient)
        DetailRow("UPI ID", state.recipient)
        if (state.note.isNotBlank()) DetailRow("Note", state.note)
        DetailRow("From", "Savings •• 4821")
        DetailRow("Fee", "Free")
        DetailRow("Reference", state.idempotencyKey)
    }
    when (state.status) {
        TransferStatus.PENDING -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text(
                if (state.attempt <= 1) "Sending…" else "Weak connection. Trying again (${state.attempt} of $TRANSACTION_ATTEMPTS)…",
            )
        }
        TransferStatus.IN_DOUBT -> {
            StatusBanner(
                state.errorMessage ?: stringResource(AppStrings.MESSAGE_TRANSFER_UNCONFIRMED),
                BannerTone.WARNING,
                title = stringResource(AppStrings.TITLE_TRANSFER_UNCONFIRMED),
            )
            PrimaryButton(
                stringResource(AppStrings.ACTION_CHECK_STATUS),
                onClick = viewModel::onCheckStatusClicked,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = viewModel::onRetryClicked,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(AppStrings.ACTION_RETRY))
            }
        }
        TransferStatus.FAILED -> {
            StatusBanner(formatApiError(state.errorMessage.orEmpty()), BannerTone.ERROR, title = "Payment Failed")
            PrimaryButton(
                stringResource(AppStrings.ACTION_RETRY),
                onClick = viewModel::onRetryClicked,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        else -> {
            StatusBanner("We'll ask for your fingerprint or screen lock before sending.", BannerTone.INFO)
            StepUpButton(debugStepUp, viewModel::onStepUpAuthClicked)
        }
    }
    if (state.status != TransferStatus.PENDING) {
        TextButton(onClick = viewModel::onEditDetailsClicked, modifier = Modifier.fillMaxWidth()) { Text(stringResource(AppStrings.ACTION_EDIT_DETAILS)) }
    }
}

/** Shows transfer receipt. */
@Composable
private fun TransferReceipt(state: WalletUiState, onDone: () -> Unit, viewModel: WalletViewModel) {
    val contact = MockCatalog.contacts.firstOrNull { it.handle == state.recipient }
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBadge(Icons.Outlined.CheckCircle, size = 88)
        Text(stringResource(AppStrings.LABEL_MONEY_SENT), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(state.postedMinor), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("to ${contact?.name ?: state.recipient}", style = MaterialTheme.typography.bodyLarge)
    }
    StatusBanner("Payment of ${formatMoney(state.postedMinor)} sent successfully.", BannerTone.SUCCESS, title = "Payment Successful")
    SectionCard {
        DetailRow("Status", "Completed")
        DetailRow("Reference", state.idempotencyKey)
        DetailRow("Remaining balance", formatMoney(state.balanceMinor))
    }
    PrimaryButton("Done", onClick = { viewModel.onDoneClicked(onDone) })
    OutlinedButton(onClick = viewModel::onSendAnotherClicked, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(AppStrings.ACTION_SEND_ANOTHER))
    }
}

/** Backward-compatible overload for previews. */
@Composable
private fun TransferReceipt(state: WalletUiState, onDone: () -> Unit, onSendAnother: () -> Unit) {
    val contact = MockCatalog.contacts.firstOrNull { it.handle == state.recipient }
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBadge(Icons.Outlined.CheckCircle, size = 88)
        Text(stringResource(AppStrings.LABEL_MONEY_SENT), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(formatMoney(state.postedMinor), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
        Text("to ${contact?.name ?: state.recipient}", style = MaterialTheme.typography.bodyLarge)
    }
    SectionCard {
        DetailRow("Status", "Completed")
        DetailRow("Reference", state.idempotencyKey)
        DetailRow("Remaining balance", formatMoney(state.balanceMinor))
    }
    PrimaryButton("Done", onClick = onDone)
    OutlinedButton(onClick = onSendAnother, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(AppStrings.ACTION_SEND_ANOTHER))
    }
}

/** Shows step up button. */
@Composable
private fun StepUpButton(debugStepUp: Boolean, onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    if (debugStepUp) {
        PrimaryButton("Confirm (debug build)", onClick = { onResult(true) }, icon = Icons.Outlined.Lock)
        return
    }
    PrimaryButton(
        "Confirm and send",
        onClick = { (context as? FragmentActivity)?.let { authenticate(it, onResult) } },
        icon = Icons.Outlined.Fingerprint,
    )
}

/** Authenticate. */
private fun authenticate(activity: FragmentActivity, onResult: (Boolean) -> Unit) {
    val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            /** Reacts to authentication succeeded. */
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
            /** Reacts to authentication error. */
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
        },
    )
    val strong = BiometricManager.Authenticators.BIOMETRIC_STRONG
    val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        strong or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
        strong
    }
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Confirm transfer")
        .setAllowedAuthenticators(authenticators)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        info.setNegativeButtonText("Cancel")
    }
    prompt.authenticate(info.build())
}

private val previewWallet = WalletViewModel(PaymentRail { 0L })

private val reviewState = WalletUiState(
    balanceMinor = 2_45_678_90L,
    amountMinor = 50_000L,
    recipient = "asha@okaxis",
    note = "Dinner",
    step = TransferStep.REVIEW,
    status = TransferStatus.STEP_UP,
)

@PreviewLightDark
@Composable
private fun WalletPreview() {
    PersonalLedgerTheme { WalletScreen(onBack = {}, viewModel = previewWallet) }
}

@PreviewLightDark
@Composable
private fun WalletInsufficientPreview() {
    PersonalLedgerTheme {
        FeatureScaffold("Send money", onBack = {}) {
            TransferForm(
                reviewState.copy(status = TransferStatus.INSUFFICIENT, errorMessage = "Insufficient funds"),
                previewWallet,
                offline = false,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WalletReviewPreview() {
    PersonalLedgerTheme {
        FeatureScaffold("Review transfer", onBack = {}) {
            TransferReview(reviewState, debugStepUp = true, previewWallet)
        }
    }
}

@PreviewLightDark
@Composable
private fun WalletFailedPreview() {
    PersonalLedgerTheme {
        FeatureScaffold("Review transfer", onBack = {}) {
            TransferReview(
                reviewState.copy(status = TransferStatus.FAILED, errorMessage = "The bank did not confirm this payment."),
                debugStepUp = true,
                previewWallet,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WalletReceiptPreview() {
    PersonalLedgerTheme {
        FeatureScaffold("Transfer receipt", onBack = {}) {
            TransferReceipt(
                reviewState.copy(
                    step = TransferStep.RECEIPT,
                    status = TransferStatus.DONE,
                    postedMinor = 50_000L,
                    balanceMinor = 2_45_178_90L,
                ),
                onDone = {},
                onSendAnother = {},
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun WalletInDoubtPreview() {
    PersonalLedgerTheme {
        FeatureScaffold("Review transfer", onBack = {}) {
            TransferReview(
                reviewState.copy(
                    status = TransferStatus.IN_DOUBT,
                    errorMessage = "Connection lost while confirming. We saved your reference so you won't be charged twice.",
                    canReconcile = true,
                ),
                debugStepUp = true,
                previewWallet,
            )
        }
    }
}

