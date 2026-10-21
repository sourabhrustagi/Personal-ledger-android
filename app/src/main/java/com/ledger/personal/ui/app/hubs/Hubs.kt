package com.ledger.personal.ui.app.hubs

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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.domain.board.PromoOffer
import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.ui.app.HubFlags
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.PaymentsOfflineNotice
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.navigation.Screen
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows profile action. */
@Composable
internal fun ProfileAction(onProfile: () -> Unit) {
    IconButton(onClick = onProfile) { Icon(Icons.Outlined.AccountCircle, contentDescription = "Profile and settings") }
}


/** Shows pay hub. */
@Composable
fun PayHub(
    flags: HubFlags,
    onProfile: () -> Unit,
    onOpen: (Screen) -> Unit,
) {
    val context = LocalContext.current
    var recipientInput by rememberSaveable { mutableStateOf("") }

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
                                recipientInput = clean
                            }
                        }
                    }
                } catch (_: SecurityException) {
                    // Ignore gracefully
                }
            }
        }
    }

    FeatureScaffold("Pay", onBack = null, actions = { ProfileAction(onProfile) }) {
        if (flags.offline) PaymentsOfflineNotice()
        val online = !flags.offline

        SectionCard(title = "Scan and pay") {
            ListRow(
                icon = Icons.Outlined.QrCodeScanner,
                title = "Scan any UPI QR code",
                subtitle = "Opens camera to scan QR and make payment",
                iconTint = if (online) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                onClick = { if (online) onOpen(Screen.Scan) },
            )
            PrimaryButton(
                text = "Open camera & scan",
                icon = Icons.Outlined.QrCodeScanner,
                enabled = online,
                onClick = { onOpen(Screen.Scan) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard(title = "Send to anyone") {
            Text(
                "Enter mobile number or UPI ID, or select from local contacts",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = recipientInput,
                onValueChange = { recipientInput = it },
                label = { Text("Mobile number or UPI ID") },
                placeholder = { Text("e.g. 9876543210 or name@upi") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPicker.launch(intent)
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
                        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                        contactPicker.launch(intent)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Contacts")
                }
                PrimaryButton(
                    text = "Continue",
                    enabled = online && recipientInput.isNotBlank(),
                    onClick = { onOpen(Screen.PayTo(recipientInput.trim(), 0L)) },
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            Text(
                "Saved contacts",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            MockCatalog.contacts.forEach { contact ->
                ListRow(
                    icon = Icons.Outlined.Person,
                    title = contact.name,
                    subtitle = contact.handle,
                    iconTint = MaterialTheme.colorScheme.primary,
                    onClick = { onOpen(Screen.PayTo(contact.handle, 0L)) },
                )
            }
        }
    }
}

/** Shows the promotions stored on the device. */
@Composable
fun PromotionsHub(shelves: List<PromoTab>, selected: String, onSelect: (String) -> Unit, onProfile: () -> Unit) {
    FeatureScaffold("Offers", onBack = null, actions = { ProfileAction(onProfile) }) {
        if (shelves.isEmpty()) {
            Text("No promotions right now.")
        } else {
            val index = shelves.indexOfFirst { it.id == selected }.coerceAtLeast(0)
            PrimaryScrollableTabRow(selectedTabIndex = index, edgePadding = 16.dp) {
                shelves.forEachIndexed { i, shelf ->
                    Tab(selected = i == index, onClick = { onSelect(shelf.id) }, text = { Text(shelf.title) })
                }
            }
            val offers = shelves[index].offers
            if (offers.isEmpty()) {
                Text("Nothing in this tab.")
            } else {
                offers.forEach { offer ->
                    ListRow(Icons.Outlined.LocalOffer, offer.pitch, subtitle = offer.until.ifBlank { null })
                }
            }
        }
    }
}

private val previewFlags = HubFlags(offline = false, empty = false, reveal = true, onToggleReveal = {})

@PreviewLightDark
@Composable
private fun PayHubPreview() {
    PersonalLedgerTheme {
        PayHub(previewFlags, onProfile = {}, onOpen = {})
    }
}

@PreviewLightDark
@Composable
private fun PayHubOfflinePreview() {
    PersonalLedgerTheme {
        PayHub(
            HubFlags(offline = true, empty = false, reveal = true, onToggleReveal = {}),
            onProfile = {},
            onOpen = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PromotionsPreview() {
    PersonalLedgerTheme {
        PromotionsHub(
            shelves = listOf(PromoTab("bills", "Bills", listOf(PromoOffer("electricity", "₹25 cashback on electricity", "This week")))),
            selected = "bills",
            onSelect = {},
            onProfile = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PromotionsEmptyPreview() {
    PersonalLedgerTheme { PromotionsHub(shelves = emptyList(), selected = "", onSelect = {}, onProfile = {}) }
}
