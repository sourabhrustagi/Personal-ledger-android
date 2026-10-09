package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.ledger.personal.core.splitEvenly
import com.ledger.personal.core.upiPayload
import com.ledger.personal.mock.MockCatalog
import java.math.BigDecimal

const val MY_VPA = "me@ledgerone"

/** Shares per person, "You" first; the remainder paise go to the earliest people so the total is exact. */
fun splitShares(totalMinor: Long, people: List<String>): List<Pair<String, Long>> =
    people.zip(splitEvenly(totalMinor, people.size))

@Composable
fun SplitBillScreen(onBack: () -> Unit, initialMinor: Long = 0L, note: String = "") {
    var amount by rememberSaveable {
        mutableStateOf(if (initialMinor > 0) BigDecimal.valueOf(initialMinor, 2).stripTrailingZeros().toPlainString() else "")
    }
    var picked by rememberSaveable { mutableStateOf(listOf<String>()) }
    var sent by rememberSaveable { mutableStateOf(false) }
    val total = parseMinor(amount)
    val people = listOf("You") + picked
    val shares = if (total != null && picked.isNotEmpty()) splitShares(total, people) else emptyList()
    FeatureScaffold("Split a bill", onBack, subtitle = note.ifBlank { null }) {
        if (sent) {
            StatusBanner("Requests sent to ${picked.size} people. You'll be notified as they pay.", BannerTone.SUCCESS, title = "Requests sent")
            SectionCard(title = "Requested") {
                shares.drop(1).forEach { (handle, share) ->
                    ListRow(Icons.Outlined.CheckCircle, nameFor(handle), subtitle = upiPayload(MY_VPA, share), trailing = formatMoney(share))
                }
            }
            PrimaryButton("Done", onClick = onBack, modifier = Modifier.fillMaxWidth())
            return@FeatureScaffold
        }
        SectionCard(title = "Bill amount") {
            AmountField(amount, { amount = it }, label = "Total")
        }
        SectionCard(title = "Split with") {
            MockCatalog.contacts.forEach { contact ->
                val checked = contact.handle in picked
                Row(
                    Modifier.toggleable(checked, role = Role.Checkbox) { on ->
                        picked = if (on) picked + contact.handle else picked - contact.handle
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ListRow(Icons.Outlined.Person, contact.name, Modifier.weight(1f), subtitle = contact.handle)
                    Checkbox(checked, onCheckedChange = null)
                }
            }
        }
        if (shares.isNotEmpty()) {
            SectionCard(title = "Equal split · ${people.size} people") {
                shares.forEach { (who, share) -> DetailRow(if (who == "You") "You pay" else nameFor(who), formatMoney(share)) }
            }
        } else {
            Text("Enter an amount and pick at least one person.")
        }
        PrimaryButton(
            "Send ${picked.size} requests",
            onClick = { sent = true },
            enabled = shares.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private fun nameFor(handle: String): String = MockCatalog.contacts.firstOrNull { it.handle == handle }?.name ?: handle
