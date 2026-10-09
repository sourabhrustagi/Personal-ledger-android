package com.ledger.personal.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ledger.personal.data.tools.statementFor
import com.ledger.personal.ui.feature.AmountText
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.DetailRow
import com.ledger.personal.ui.feature.EmptyState
import com.ledger.personal.ui.feature.FeatureScaffold
import com.ledger.personal.ui.feature.ListRow
import com.ledger.personal.ui.feature.MoneyDirection
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.SectionCard
import com.ledger.personal.ui.feature.StatusBanner
import com.ledger.personal.ui.feature.categoryIcon
import com.ledger.personal.ui.screens.create.DEFAULT_CATEGORIES
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private const val STATEMENT_MONTHS = 6L

@Composable
fun StatementsScreen(onBack: () -> Unit, tools: ToolsViewModel) {
    val context = LocalContext.current
    val all by tools.transactions.collectAsStateWithLifecycle()
    val months = remember { (0 until STATEMENT_MONTHS).map { YearMonth.now().minusMonths(it) } }
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val summary = statementFor(all, YearMonth.parse(month))
    FeatureScaffold("Statements", onBack, subtitle = "Savings •• 4821") {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(months) { option ->
                FilterChip(option.toString() == month, { month = option.toString() }, label = {
                    Text(option.format(DateTimeFormatter.ofPattern("MMM yyyy")))
                })
            }
        }
        SectionCard(title = "Summary") {
            DetailRow("Transactions", summary.rows.size.toString())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Money in")
                AmountText(summary.inMinor, MoneyDirection.CREDIT, label = "money in")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Money out")
                AmountText(summary.outMinor, MoneyDirection.DEBIT, label = "money out")
            }
        }
        error?.let { StatusBanner(it, BannerTone.ERROR) }
        if (summary.rows.isEmpty()) {
            EmptyState(Icons.Outlined.Description, "Nothing this month", "Rupee transactions in this month will appear on the statement.")
        }
        PrimaryButton(
            "Download PDF",
            onClick = {
                error = null
                tools.shareStatement(context, summary) { error = it }
            },
            icon = Icons.Outlined.Description,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "The PDF opens in the share sheet so you can save it to Files or send it.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CategoriesScreen(categories: List<String>, onBack: () -> Unit, onSave: (List<String>) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    val name = draft.trim()
    val duplicate = categories.any { it.equals(name, ignoreCase = true) && it != renaming }
    FeatureScaffold("Categories", onBack, subtitle = "${categories.size} categories") {
        SectionCard(title = if (renaming == null) "Add a category" else "Rename $renaming") {
            OutlinedTextField(
                draft,
                { if (it.length <= 24) draft = it },
                label = { Text("Name") },
                isError = duplicate,
                supportingText = { if (duplicate) Text("That category already exists") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                if (renaming == null) "Add" else "Save name",
                onClick = {
                    onSave(renaming?.let { old -> categories.map { if (it == old) name else it } } ?: (categories + name))
                    draft = ""
                    renaming = null
                },
                enabled = name.isNotEmpty() && !duplicate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SectionCard(title = "Your categories") {
            categories.forEach { category ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ListRow(categoryIcon(category), category, Modifier.weight(1f), subtitle = "Tap to rename", onClick = {
                        renaming = category
                        draft = category
                    })
                    IconButton(onClick = { onSave(categories - category) }, enabled = categories.size > 1 && category != "Salary") {
                        Icon(Icons.Outlined.Delete, contentDescription = "Delete $category")
                    }
                }
            }
        }
        ListRow(
            Icons.AutoMirrored.Outlined.Label,
            "Restore defaults",
            subtitle = DEFAULT_CATEGORIES.joinToString(),
            onClick = { onSave(DEFAULT_CATEGORIES) },
        )
        Text(
            "Salary can't be removed because it marks money coming in. Renaming doesn't change past transactions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
