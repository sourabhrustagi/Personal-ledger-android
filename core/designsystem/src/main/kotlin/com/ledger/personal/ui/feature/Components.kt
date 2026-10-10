package com.ledger.personal.ui.feature

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.ledger.personal.ui.components.LedgerOneButton
import com.ledger.personal.ui.components.LedgerOneCard
import com.ledger.personal.ui.components.LedgerOneText
import com.ledger.personal.ui.components.LedgerOneTextField
import com.ledger.personal.ui.components.LedgerOneTextRole
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.LedgerTheme

/** Allowed values of banner tone. */
enum class BannerTone { INFO, SUCCESS, WARNING, ERROR }

/** Tone color. */
@Composable
fun toneColor(tone: BannerTone): Color = when (tone) {
    BannerTone.INFO -> MaterialTheme.colorScheme.secondary
    BannerTone.SUCCESS -> LedgerTheme.colors.success
    BannerTone.WARNING -> LedgerTheme.colors.warning
    BannerTone.ERROR -> MaterialTheme.colorScheme.error
}

/** Shows hero card. */
@Composable
fun HeroCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    val onHero = colors.onPrimary
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.primary)
            .padding(20.dp),
    ) {
        CompositionLocalProvider(LocalContentColor provides onHero) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LedgerOneText(label, role = LedgerOneTextRole.Label, color = onHero)
                LedgerOneText(value, role = LedgerOneTextRole.Display, weight = FontWeight.Bold, color = onHero)
                supporting?.let { LedgerOneText(it, color = onHero) }
                footer()
            }
        }
    }
}

/** Shows section card. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    LedgerOneCard(modifier) {
        title?.let { LedgerOneText(it, role = LedgerOneTextRole.Title, weight = FontWeight.SemiBold) }
        content()
    }
}

/** Shows section header. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        action()
    }
}

/** Shows icon badge. */
@Composable
fun IconBadge(icon: ImageVector, modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary, size: Int = 44) {
    Box(
        modifier.size(size.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size / 2).dp))
    }
}

/** Shows list row. */
@Composable
fun ListRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: String? = null,
    trailingColor: Color = MaterialTheme.colorScheme.onSurface,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = 56.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(icon, tint = iconTint)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        trailing?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = trailingColor) }
        trailingContent?.invoke()
    }
}

/** Shows detail row. */
@Composable
fun DetailRow(label: String, value: String, modifier: Modifier = Modifier, emphasize: Boolean = false) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

/** Shows status banner. */
@Composable
fun StatusBanner(message: String, tone: BannerTone, modifier: Modifier = Modifier, title: String? = null) {
    val color = toneColor(tone)
    val icon = when (tone) {
        BannerTone.INFO -> Icons.Outlined.Info
        BannerTone.SUCCESS -> Icons.Outlined.CheckCircle
        BannerTone.WARNING -> Icons.Outlined.WarningAmber
        BannerTone.ERROR -> Icons.Outlined.ErrorOutline
    }
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.12f)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = color)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                title?.let { Text(it, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold) }
                Text(message, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Shows status chip. */
@Composable
fun StatusChip(text: String, tone: BannerTone, modifier: Modifier = Modifier) {
    val color = toneColor(tone)
    Surface(modifier, shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Shows empty state. */
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        IconBadge(icon, size = 64, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

/** Shows primary button. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: ImageVector? = null) {
    LedgerOneButton(text, onClick, modifier, enabled, icon)
}

/** Shows amount field. */
@Composable
fun AmountField(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Amount",
    symbol: String = "₹",
    supportingText: String? = null,
    isError: Boolean = false,
) {
    LedgerOneTextField(
        value = text,
        onValueChange = { value -> if (amountInput.matches(value)) onTextChange(value) },
        modifier = modifier,
        label = label,
        prefix = symbol,
        supportingText = supportingText,
        isError = isError,
        keyboardType = KeyboardType.Decimal,
    )
}

private val amountInput = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

/** Category icon. */
fun categoryIcon(category: String): ImageVector = when (category.lowercase()) {
    "groceries" -> Icons.Outlined.ShoppingCart
    "dining" -> Icons.Outlined.Restaurant
    "transport" -> Icons.Outlined.DirectionsCar
    "salary" -> Icons.Outlined.Payments
    "utilities" -> Icons.Outlined.Bolt
    "entertainment" -> Icons.Outlined.Movie
    else -> Icons.AutoMirrored.Outlined.ReceiptLong
}
