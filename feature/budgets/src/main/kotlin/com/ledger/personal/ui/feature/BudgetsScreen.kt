package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.personal.mock.MockCatalog
import com.ledger.personal.ui.theme.LedgerTheme

private const val NEAR_LIMIT = 0.8f
private val seedFactors = listOf(1.25, 0.9, 1.5, 1.05)

/** Month spend by category. */
fun monthSpendByCategory(): Map<String, Long> = MockCatalog.transactions
    .filter { it.currency == "INR" && it.category != "Salary" && it.dayOffset < 30 }
    .groupBy { it.category }
    .mapValues { (_, rows) -> rows.sumOf { it.amountMinor } }

/** Seed limit. */
private fun seedLimit(spent: Long, index: Int): Long {
    val raw = (spent * seedFactors[index % seedFactors.size]).toLong()
    return ((raw + 99_999) / 1_00_000) * 1_00_000
}

/** Shows the budgets screen. */
@Composable
fun BudgetsScreen(onBack: () -> Unit, onInsights: () -> Unit) {
    val spent = remember { monthSpendByCategory() }
    val limits = remember {
        mutableStateMapOf<String, Long>().apply {
            spent.entries.sortedByDescending { it.value }.forEachIndexed { i, (cat, value) -> put(cat, seedLimit(value, i)) }
        }
    }
    var newCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var newLimit by rememberSaveable { mutableStateOf("") }
    val totalLimit = limits.values.sum()
    val totalSpent = limits.keys.sumOf { spent[it] ?: 0 }
    FeatureScaffold("Budgets", onBack, subtitle = "This month") {
        HeroCard(
            label = "Spent of ${formatMoney(totalLimit)}",
            value = formatMoney(totalSpent),
            supporting = "${formatMoney((totalLimit - totalSpent).coerceAtLeast(0))} left across ${limits.size} budgets",
        )
        if (limits.isEmpty()) {
            EmptyState(Icons.Outlined.CheckCircle, "No budgets yet", "Pick a category below and set a monthly limit.")
        }
        SectionCard(title = "Category budgets") {
            limits.entries.sortedByDescending { (spent[it.key] ?: 0).toFloat() / it.value }.forEach { (cat, limit) ->
                BudgetRow(cat, spent[cat] ?: 0, limit)
            }
        }
        SectionCard(title = "Create or change a budget") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(spent.keys.sorted()) { cat ->
                    FilterChip(cat == newCategory, {
                        newCategory = cat
                        newLimit = limits[cat]?.let { (it / 100).toString() }.orEmpty()
                    }, label = { Text(cat) })
                }
            }
            AmountField(newLimit, { newLimit = it }, label = "Monthly limit")
            PrimaryButton(
                if (newCategory != null && limits.containsKey(newCategory)) "Update budget" else "Save budget",
                onClick = {
                    val cat = newCategory ?: return@PrimaryButton
                    limits[cat] = parseMinor(newLimit) ?: return@PrimaryButton
                    newCategory = null
                    newLimit = ""
                },
                enabled = newCategory != null && parseMinor(newLimit) != null,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        TextLink("See spending charts", onInsights)
    }
}

/** Shows budget row. */
@Composable
private fun BudgetRow(category: String, spentMinor: Long, limitMinor: Long) {
    val ratio = if (limitMinor == 0L) 0f else spentMinor.toFloat() / limitMinor
    val (color, icon, status) = when {
        ratio > 1f -> Triple(MaterialTheme.colorScheme.error, Icons.Outlined.ErrorOutline, "Over by ${formatMoney(spentMinor - limitMinor)}")
        ratio >= NEAR_LIMIT -> Triple(LedgerTheme.colors.warning, Icons.Outlined.WarningAmber, "Near limit · ${formatMoney(limitMinor - spentMinor)} left")
        else -> Triple(LedgerTheme.colors.success, Icons.Outlined.CheckCircle, "${formatMoney(limitMinor - spentMinor)} left")
    }
    Column(
        Modifier.fillMaxWidth().padding(vertical = 4.dp).clearAndSetSemantics {
            contentDescription = "$category budget: spent ${spokenAmount(spentMinor)} of ${spokenAmount(limitMinor)}. $status"
        },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(categoryIcon(category), size = 40)
            Column(Modifier.weight(1f)) {
                Text(category, fontWeight = FontWeight.Medium)
                Text("${formatMoney(spentMinor)} of ${formatMoney(limitMinor)}", style = MaterialTheme.typography.bodySmall)
            }
            Text("${(ratio * 100).toInt()}%", fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(progress = { ratio.coerceAtMost(1f) }, color = color, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(end = 2.dp))
            Text(status, style = MaterialTheme.typography.bodySmall, color = color)
        }
    }
}
