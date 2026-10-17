package com.ledger.personal.ui.feature

import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Keeps reduced motion. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/** Shows skeleton. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, height: Dp = 16.dp) {
    val reduced = rememberReducedMotion()
    val alpha = if (reduced) {
        0.6f
    } else {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val pulse by transition.animateFloat(0.35f, 0.8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse")
        pulse
    }
    Box(
        modifier.height(height).clip(RoundedCornerShape(8.dp)).alpha(alpha).background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

/** Shows skeleton rows. */
@Composable
fun SkeletonRows(count: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier.semantics { contentDescription = "Loading" }, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(count) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Skeleton(Modifier.size(44.dp).clip(CircleShape), height = 44.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Skeleton(Modifier.fillMaxWidth(0.6f))
                    Skeleton(Modifier.fillMaxWidth(0.35f), height = 12.dp)
                }
                Skeleton(Modifier.size(width = 72.dp, height = 16.dp))
            }
        }
    }
}

/** Shows offline banner. */
@Composable
fun OfflineBanner(ageLabel: String?, modifier: Modifier = Modifier) {
    StatusBanner(
        message = ageLabel?.let { "You're offline. Showing the last saved copy from $it." } ?: "You're offline. Showing the last saved copy.",
        tone = BannerTone.WARNING,
        title = "No connection",
        modifier = modifier,
    )
}

/** Shows payments offline notice. */
@Composable
fun PaymentsOfflineNotice(modifier: Modifier = Modifier) {
    StatusBanner("Payments need a live, secure connection. Try again when you're back online.", BannerTone.WARNING, modifier, title = "Payments paused")
}

/** Allowed values of sync state. */
enum class SyncState { SYNCED, SYNCING, WAITING, FAILED }

/** Shows sync indicator. */
@Composable
fun SyncIndicator(state: SyncState, modifier: Modifier = Modifier) {
    val (icon, label, tone) = when (state) {
        SyncState.SYNCED -> Triple(Icons.Outlined.CloudDone, "Synced", BannerTone.SUCCESS)
        SyncState.SYNCING -> Triple(Icons.Outlined.Sync, "Syncing", BannerTone.INFO)
        SyncState.WAITING -> Triple(Icons.Outlined.CloudOff, "Waiting for network", BannerTone.WARNING)
        SyncState.FAILED -> Triple(Icons.Outlined.ErrorOutline, "Couldn't reach the bank", BannerTone.ERROR)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(icon, contentDescription = null, tint = toneColor(tone), modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shows step header. */
@Composable
fun StepHeader(steps: List<String>, current: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            steps.indices.forEach { index ->
                Box(
                    Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(
                        if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }
        Text(
            "Step ${current + 1} of ${steps.size} · ${steps[current]}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Timeline step. */
data class TimelineStep(val label: String, val detail: String?, val done: Boolean)

/** Shows timeline. */
@Composable
fun Timeline(steps: List<TimelineStep>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEach { step ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.padding(top = 4.dp).size(12.dp).clip(CircleShape).background(
                        if (step.done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                )
                Column {
                    Text(step.label, fontWeight = if (step.done) FontWeight.SemiBold else FontWeight.Normal)
                    step.detail?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun StatesPreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonRows(count = 2)
            OfflineBanner("2:14 PM")
            PaymentsOfflineNotice()
            SyncIndicator(SyncState.SYNCING)
            StepHeader(listOf("Details", "Scan", "Review"), current = 1)
            Timeline(
                listOf(
                    TimelineStep("Submitted", "We've logged your dispute", true),
                    TimelineStep("Under review", null, false),
                ),
            )
        }
    }
}
