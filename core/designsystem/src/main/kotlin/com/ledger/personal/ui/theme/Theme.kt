@file:OptIn(ExperimentalTextApi::class)

package com.ledger.personal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledger.personal.designsystem.R

/** Allowed values of theme mode. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ledger colors. */
@Immutable
data class LedgerColors(
    val success: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val danger: Color,
    val dangerContainer: Color,
)

private val LightLedger = LedgerColors(Green700, Green100, Amber700, Amber100, Red700, Red100)
private val DarkLedger = LedgerColors(Green400, Green950, Amber400, Amber950, Red300, Red950)

val LocalLedgerColors = staticCompositionLocalOf { LightLedger }

/** Shared ledger theme. */
object LedgerTheme {
    val colors: LedgerColors
        @Composable get() = LocalLedgerColors.current
}

private val LightScheme = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue100,
    onPrimaryContainer = Blue900,
    secondary = Slate600,
    onSecondary = Color.White,
    secondaryContainer = Slate100,
    onSecondaryContainer = Ink,
    tertiary = Amber700,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Slate100,
    outline = Slate300,
    outlineVariant = Slate200,
    error = Red700,
    onError = Color.White,
    errorContainer = Red100,
    onErrorContainer = Red700,
)

private val DarkScheme = darkColorScheme(
    primary = Blue300,
    onPrimary = BlueInk,
    primaryContainer = Blue900,
    onPrimaryContainer = Blue100,
    secondary = Slate300,
    onSecondary = Slate950,
    secondaryContainer = Slate850,
    onSecondaryContainer = Slate100,
    tertiary = Amber400,
    onTertiary = Slate950,
    background = Slate950,
    onBackground = Slate100,
    surface = Slate900,
    onSurface = Slate100,
    surfaceVariant = Slate850,
    onSurfaceVariant = Slate300,
    surfaceContainer = Slate900,
    surfaceContainerHigh = Slate850,
    outline = Slate700,
    outlineVariant = Slate850,
    error = Red300,
    onError = Red950,
    errorContainer = Red950,
    onErrorContainer = Red300,
)

private fun inter(weight: FontWeight) = Font(
    R.font.inter,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private val Inter = FontFamily(
    inter(FontWeight.Normal),
    inter(FontWeight.Medium),
    inter(FontWeight.SemiBold),
    inter(FontWeight.Bold),
)

/** Tabular figures in Inter. */
private fun TextStyle.ledger(size: Int? = null) = copy(
    fontFamily = Inter,
    fontFeatureSettings = "tnum",
    fontSize = size?.sp ?: fontSize,
    lineHeight = size?.let { (it + 8).sp } ?: lineHeight,
)

private val LedgerTypography = Typography().run {
    copy(
        displayLarge = displayLarge.ledger(),
        displayMedium = displayMedium.ledger(),
        displaySmall = displaySmall.ledger(),
        headlineLarge = headlineLarge.ledger(),
        headlineMedium = headlineMedium.ledger(),
        headlineSmall = headlineSmall.ledger(),
        titleLarge = titleLarge.ledger(22),
        titleMedium = titleMedium.ledger(18),
        titleSmall = titleSmall.ledger(16),
        bodyLarge = bodyLarge.ledger(16),
        bodyMedium = bodyMedium.ledger(16),
        bodySmall = bodySmall.ledger(14),
        labelLarge = labelLarge.ledger(16),
        labelMedium = labelMedium.ledger(14),
        labelSmall = labelSmall.ledger(14),
    )
}

private val LedgerShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(16.dp),
)

/** Shows personal ledger theme. */
@Composable
fun PersonalLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val preview = LocalInspectionMode.current
    CompositionLocalProvider(LocalLedgerColors provides if (darkTheme) DarkLedger else LightLedger) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = if (preview) Typography() else LedgerTypography,
            shapes = LedgerShapes,
        ) {
            if (preview) Surface(color = MaterialTheme.colorScheme.background, content = content) else content()
        }
    }
}

/** Shows ledger one theme. */
@Composable
fun LedgerOneTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    PersonalLedgerTheme(darkTheme = dark, content = content)
}
