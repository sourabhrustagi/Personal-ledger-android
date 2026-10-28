package com.ledger.personal.feature.hubs.pay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.ui.components.FeatureScaffold
import com.ledger.personal.ui.components.HubFlags
import com.ledger.personal.ui.components.ListRow
import com.ledger.personal.ui.components.PaymentsOfflineNotice
import com.ledger.personal.ui.components.PrimaryButton
import com.ledger.personal.ui.components.SectionCard
import com.ledger.personal.ui.theme.AppStrings
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows pay hub with all click interactions routed through [PayHubViewModel]. */
@Composable
fun PayHub(
    flags: HubFlags,
    onScanClick: () -> Unit,
    onPayTo: (recipient: String, amount: Long) -> Unit,
    viewModel: PayHubViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val recipientInput = uiState.recipientInput

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
                                viewModel.onContactPicked(clean)
                            }
                        }
                    }
                } catch (_: SecurityException) {
                    // Ignore gracefully
                }
            }
        }
    }

    FeatureScaffold(stringResource(AppStrings.TITLE_PAY), onBack = null) {
        if (flags.offline) PaymentsOfflineNotice()
        val online = !flags.offline

        SectionCard(title = stringResource(AppStrings.SECTION_SCAN_AND_PAY)) {
            ListRow(
                icon = Icons.Outlined.QrCodeScanner,
                title = stringResource(AppStrings.ACTION_SCAN_QR),
                subtitle = "Opens camera to scan QR and make payment",
                iconTint = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                onClick = { viewModel.onScanClicked(online) { onScanClick() } },
            )
            PrimaryButton(
                text = stringResource(AppStrings.ACTION_OPEN_CAMERA),
                icon = Icons.Outlined.QrCodeScanner,
                enabled = online,
                onClick = { viewModel.onScanClicked(online) { onScanClick() } },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = stringResource(AppStrings.SECTION_SEND_TO_ANYONE)) {
            Text(
                "Enter mobile number or UPI ID, or select from local contacts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = recipientInput,
                onValueChange = viewModel::onRecipientChanged,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.onPickContactClicked {
                            val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                            contactPicker.launch(intent)
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(AppStrings.ACTION_CONTACTS))
                }
                PrimaryButton(
                    text = stringResource(AppStrings.ACTION_CONTINUE),
                    enabled = online && recipientInput.isNotBlank(),
                    onClick = {
                        viewModel.onContinueClicked { recipient ->
                            onPayTo(recipient, 0L)
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            Text(
                stringResource(AppStrings.LABEL_SAVED_CONTACTS),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MockCatalog.contacts.forEach { contact ->
                ListRow(
                    icon = Icons.Outlined.Person,
                    title = contact.name,
                    subtitle = contact.handle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.onSavedContactClicked(contact.handle) { handle ->
                            onPayTo(handle, 0L)
                        }
                    },
                )
            }
        }
    }
}

private val previewFlags = HubFlags(offline = false, empty = false, reveal = true, onToggleReveal = {})
private val previewPayHubViewModel = PayHubViewModel()

@PreviewLightDark
@Composable
private fun PayHubPreview() {
    PersonalLedgerTheme {
        PayHub(previewFlags, onScanClick = {}, onPayTo = { _, _ -> }, viewModel = previewPayHubViewModel)
    }
}

@PreviewLightDark
@Composable
private fun PayHubOfflinePreview() {
    PersonalLedgerTheme {
        PayHub(
            HubFlags(offline = true, empty = false, reveal = true, onToggleReveal = {}),
            onScanClick = {},
            onPayTo = { _, _ -> },
            viewModel = previewPayHubViewModel,
        )
    }
}
