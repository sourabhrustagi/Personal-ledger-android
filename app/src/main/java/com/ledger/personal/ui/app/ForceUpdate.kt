package com.ledger.personal.ui.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.feature.BannerTone
import com.ledger.personal.ui.feature.IconBadge
import com.ledger.personal.ui.feature.PrimaryButton
import com.ledger.personal.ui.feature.StatusBanner

/** Blocks the app until this version is updated from the store. */
@Composable
fun ForceUpdateScreen(versionName: String) {
    val context = LocalContext.current
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.safeDrawingPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(48.dp))
            IconBadge(Icons.Outlined.SystemUpdate, size = 72)
            Text("Update required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Version $versionName is no longer supported. Update LedgerOne to keep your account secure and keep paying.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusBanner("Your money and data are safe. Nothing is lost by updating.", BannerTone.INFO)
            PrimaryButton(
                "Update now",
                onClick = { openStore(context) },
                icon = Icons.Outlined.SystemUpdate,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Opens the Play listing for this app. */
private fun openStore(context: Context) {
    val id = context.packageName.removeSuffix(".debug")
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")))
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")))
    }
}
