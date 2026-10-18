package com.ledger.personal.ui.feature

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows the charts screen. */
@Composable
fun ChartsScreen(onBack: () -> Unit, viewModel: ChartsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val palette = chartPalette()
    FeatureScaffold("Spending insights", onBack, subtitle = "From your ledger") {
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf("INR", "USD").forEachIndexed { index, currency ->
                SegmentedButton(
                    selected = state.currency == currency,
                    onClick = { viewModel.selectCurrency(currency) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) { Text(if (currency == "INR") "₹ INR account" else "$ USD account") }
            }
        }
        HeroCard(
            label = if (state.category == "All") "Total spent" else "Spent on ${state.category}",
            value = formatMoney(state.totalMinor, state.currency),
            supporting = "Income ${formatMoney(state.incomeMinor, state.currency)}",
        )
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf("All") + state.categories).forEach { category ->
                FilterChip(
                    selected = state.category == category,
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category) },
                )
            }
        }
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(ChartTab.CATEGORY to "By category", ChartTab.MONTH to "By month").forEachIndexed { index, (tab, label) ->
                SegmentedButton(
                    selected = state.tab == tab,
                    onClick = { viewModel.selectTab(tab) },
                    shape = SegmentedButtonDefaults.itemShape(index, 2),
                ) { Text(label) }
            }
        }
        LoadingOrEmpty(state.isLoading, state.isEmpty, state.errorMessage, "spending in this view")
        if (state.slices.isNotEmpty()) {
            if (state.tab == ChartTab.CATEGORY) {
                SectionCard { Donut(state.slices, palette, state.totalMinor, state.currency) }
            }
            SectionCard(title = if (state.tab == ChartTab.CATEGORY) "Categories" else "Months") {
                val max = state.slices.maxOf { it.amountMinor }.coerceAtLeast(1L)
                state.slices.forEachIndexed { index, slice ->
                    SliceRow(slice, palette[index % palette.size], slice.amountMinor.toFloat() / max, state.currency, state.tab)
                }
            }
        }
    }
}

/** Chart palette. */
@Composable
private fun chartPalette(): List<Color> = with(MaterialTheme.colorScheme) {
    listOf(primary, secondary, tertiary, error, onSurfaceVariant)
}

/** Shows donut. */
@Composable
private fun Donut(slices: List<Slice>, palette: List<Color>, totalMinor: Long, currency: String) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
        val summary = "Spending chart, total ${formatMoney(totalMinor, currency)}: " +
            slices.joinToString { "${it.label} ${formatMoney(it.amountMinor, currency)}" }
        Canvas(Modifier.size(180.dp).semantics { contentDescription = summary }) {
            val stroke = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Butt)
            drawArc(track, 0f, 360f, useCenter = false, style = stroke)
            var start = -90f
            slices.forEachIndexed { index, slice ->
                val sweep = 360f * slice.amountMinor / totalMinor.coerceAtLeast(1L)
                drawArc(palette[index % palette.size], start, sweep - 1f, useCenter = false, style = stroke)
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatMoney(totalMinor, currency), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Shows slice row. */
@Composable
private fun SliceRow(slice: Slice, color: Color, fraction: Float, currency: String, tab: ChartTab) {
    val progress by animateFloatAsState(fraction, label = "slice")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (tab == ChartTab.CATEGORY) {
                IconBadge(categoryIcon(slice.label), tint = color, size = 36)
            } else {
                Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            }
            Column(Modifier.weight(1f)) {
                Text(slice.label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("${slice.count} transactions", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatMoney(slice.amountMinor, currency), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { progress },
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun ChartsPreview() {
    PersonalLedgerTheme { ChartsScreen(onBack = {}, viewModel = ChartsViewModel()) }
}
