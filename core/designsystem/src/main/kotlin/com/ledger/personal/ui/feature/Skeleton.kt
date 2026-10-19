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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
fun SkeletonRows(modifier: Modifier = Modifier, count: Int = 5) {
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

@PreviewLightDark
@Composable
private fun SkeletonPreview() {
    PersonalLedgerTheme {
        SkeletonRows(count = 2)
    }
}
