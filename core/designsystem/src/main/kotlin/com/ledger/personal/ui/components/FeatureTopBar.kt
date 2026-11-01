package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.theme.colorScheme
import com.ledger.personal.ui.theme.typography

/**
 * Standard top app bar for feature screens.
 * Responsible solely for rendering title, optional subtitle, back navigation, and action slots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = title,
                    style = typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                subtitle?.let {
                    Text(
                        text = it,
                        style = typography.labelMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.background,
        ),
        modifier = modifier,
    )
}

@PreviewLightDark
@Composable
private fun FeatureTopBarPreview() {
    PersonalLedgerTheme {
        FeatureTopBar(
            title = "Account & Security",
            subtitle = "Everything saved on this device",
            onBack = {},
        )
    }
}
