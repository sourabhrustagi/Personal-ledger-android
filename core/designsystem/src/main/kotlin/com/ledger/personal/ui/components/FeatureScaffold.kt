package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/**
 * Standard screen layout scaffold for feature screens.
 * Responsible solely for screen frame scaffolding, responsive width bounding, and content layout.
 */
@Composable
fun FeatureScaffold(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    scrollable: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            if (!title.isNullOrEmpty() || onBack != null) {
                FeatureTopBar(
                    title = title.orEmpty(),
                    onBack = onBack,
                    subtitle = subtitle,
                    actions = actions,
                )
            }
        },
        snackbarHost = snackbarHost,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            val base = Modifier
                .fillMaxWidth()
                .widthIn(max = 720.dp)
            Column(
                modifier = (if (scrollable) base.verticalScroll(rememberScrollState()) else base)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = content,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun FeatureScaffoldPreview() {
    PersonalLedgerTheme {
        FeatureScaffold(title = "Payments", onBack = {}, subtitle = "UPI · Instant") {
            HeroCard("Available", "₹2,45,678.90", supporting = "Savings •• 4821")
        }
    }
}
