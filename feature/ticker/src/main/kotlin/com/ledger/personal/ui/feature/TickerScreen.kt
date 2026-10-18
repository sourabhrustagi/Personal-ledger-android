package com.ledger.personal.ui.feature

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.math.RoundingMode

/** Shows the ticker screen. */
@Composable
fun TickerScreen(onBack: () -> Unit, viewModel: TickerViewModel = hiltViewModel()) {
    val tick by viewModel.ticks.collectAsStateWithLifecycle()
    val polling by viewModel.polling.collectAsStateWithLifecycle()
    val history = remember { mutableStateListOf<Long>() }
    LaunchedEffect(tick) {
        if (tick > 0L) {
            history += tick
            if (history.size > 90) history.removeAt(0)
        }
    }
    FeatureScaffold(
        "Markets",
        onBack,
        subtitle = "USD → INR",
        actions = {
            StatusChip(if (polling) "Delayed" else "Live", if (polling) BannerTone.WARNING else BannerTone.SUCCESS, Modifier.padding(end = 12.dp))
        },
    ) {
        TickerBody(tick, polling, history)
    }
}

/** Shows ticker body. */
@Composable
private fun ColumnScope.TickerBody(tick: Long, polling: Boolean, history: List<Long>) {
    if (tick == 0L) {
        Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text("Connecting to market feed…")
            }
        }
        return
    }
    val open = history.firstOrNull() ?: tick
    HeroCard(label = "US Dollar · 1 USD", value = formatMoney(tick), supporting = changeLabel(open, tick))
    if (polling) StatusBanner("The live feed dropped. Prices refresh every second until it reconnects.", BannerTone.WARNING)
    SectionCard(title = "Today") {
        Sparkline(history)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("Open", formatMoney(open), Modifier.weight(1f))
            Stat("High", formatMoney(history.maxOrNull() ?: tick), Modifier.weight(1f))
            Stat("Low", formatMoney(history.minOrNull() ?: tick), Modifier.weight(1f))
        }
    }
    Converter(tick)
}

/** Shows converter. */
@Composable
private fun Converter(rateMinor: Long) {
    var usdText by rememberSaveable { mutableStateOf("100") }
    val usdMinor = parseMinor(usdText) ?: 0L
    SectionCard(title = "Convert") {
        AmountField(usdText, { usdText = it }, label = "You send", symbol = "$")
        DetailRow("Recipient gets", formatMoney(usdMinor * rateMinor / 100), emphasize = true)
        DetailRow("Rate", "1 USD = ${formatMoney(rateMinor)}")
    }
}

/** Shows stat. */
@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** Shows sparkline. */
@Composable
private fun Sparkline(points: List<Long>) {
    val line = MaterialTheme.colorScheme.primary
    val summary = if (points.size < 2) {
        "Price chart, waiting for data"
    } else {
        "Price chart over the last ${points.size} ticks: low ${formatMoney(points.min())}, high ${formatMoney(points.max())}, " +
            "latest ${formatMoney(points.last())}, ${if (points.last() >= points.first()) "up" else "down"} overall"
    }
    Canvas(Modifier.fillMaxWidth().height(140.dp).semantics { contentDescription = summary }) {
        if (points.size < 2) return@Canvas
        val min = points.min()
        val span = (points.max() - min).coerceAtLeast(1L).toFloat()
        val step = size.width / (points.size - 1)
        val path = Path()
        points.forEachIndexed { index, value ->
            val point = Offset(index * step, size.height - (value - min) / span * size.height)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        drawPath(path, line, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Changes label. */
private fun changeLabel(open: Long, now: Long): String {
    val diff = now - open
    val percent = if (open == 0L) BigDecimal.ZERO else BigDecimal(diff * 100).divide(BigDecimal(open), 2, RoundingMode.HALF_UP)
    val sign = if (diff >= 0) "+" else "−"
    return "$sign${formatMoney(kotlin.math.abs(diff))} ($sign${percent.abs()}%) since you opened"
}

private val ticks = listOf(8_300L, 8_310L, 8_325L, 8_318L, 8_342L)

@Suppress("ViewModelConstructorInComposable")
@PreviewLightDark
@Composable
private fun TickerPreview() {
    PersonalLedgerTheme { TickerScreen(onBack = {}, viewModel = TickerViewModel()) }
}

@PreviewLightDark
@Composable
private fun TickerConnectingPreview() {
    PersonalLedgerTheme {
        FeatureScaffold("Markets", onBack = {}, subtitle = "USD → INR") {
            TickerBody(0L, polling = false, history = emptyList())
        }
    }
}

@PreviewLightDark
@Composable
private fun TickerLivePreview() {
    PersonalLedgerTheme {
        FeatureScaffold(
            "Markets",
            onBack = {},
            subtitle = "USD → INR",
            actions = { StatusChip("Live", BannerTone.SUCCESS, Modifier.padding(end = 12.dp)) },
        ) {
            TickerBody(8_342L, polling = false, history = ticks)
        }
    }
}

@PreviewLightDark
@Composable
private fun TickerDelayedPreview() {
    PersonalLedgerTheme {
        FeatureScaffold(
            "Markets",
            onBack = {},
            subtitle = "USD → INR",
            actions = { StatusChip("Delayed", BannerTone.WARNING, Modifier.padding(end = 12.dp)) },
        ) {
            TickerBody(8_342L, polling = true, history = ticks)
        }
    }
}
