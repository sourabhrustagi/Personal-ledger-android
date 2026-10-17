package com.ledger.personal.ui.feature

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.LedgerTheme
import com.ledger.personal.ui.theme.PersonalLedgerTheme

/** Shows amount text. */
@Composable
fun AmountText(
    minor: Long,
    direction: MoneyDirection,
    modifier: Modifier = Modifier,
    currency: String = "INR",
    style: TextStyle = MaterialTheme.typography.titleMedium,
    label: String? = null,
) {
    val color = when (direction) {
        MoneyDirection.CREDIT -> LedgerTheme.colors.success
        MoneyDirection.DEBIT -> MaterialTheme.colorScheme.onSurface
        MoneyDirection.NEUTRAL -> MaterialTheme.colorScheme.onSurface
    }
    val kind = when (direction) {
        MoneyDirection.CREDIT -> "Income"
        MoneyDirection.DEBIT -> "Expense"
        MoneyDirection.NEUTRAL -> "Amount"
    }
    Text(
        formatSigned(minor, direction, currency),
        style = style,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = listOfNotNull(kind, label, spokenAmount(minor, currency)).joinToString(", ")
        },
    )
}

/** Blocks screenshots and recents thumbnails while the calling screen is visible. */
@Composable
fun SecureWindow() {
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

/** Shows protected badge. */
@Composable
fun ProtectedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier.semantics { contentDescription = "Protected screen" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = LedgerTheme.colors.success)
        Text("Protected", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shows masked amount. */
@Composable
fun MaskedAmount(
    minor: Long,
    revealed: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    currency: String = "INR",
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (revealed) formatMoney(minor, currency) else "₹ • • • • •",
            style = style,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.semantics {
                contentDescription = if (revealed) spokenAmount(minor, currency) else "Balance hidden"
            },
        )
        IconButton(onClick = onToggle) {
            Icon(
                if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (revealed) "Hide balance" else "Show balance",
                tint = color,
            )
        }
    }
}

/** Shows pin dots. */
@Composable
fun PinDots(filled: Int, length: Int, modifier: Modifier = Modifier, error: Boolean = false) {
    val active = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Row(
        modifier.semantics { contentDescription = "$filled of $length digits entered" },
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        repeat(length) { index ->
            Box(
                Modifier.size(16.dp).clip(CircleShape)
                    .background(if (index < filled) active else MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, active, CircleShape),
            )
        }
    }
}

/** Shows numeric keypad. */
@Composable
fun NumericKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    leftKey: (@Composable () -> Unit)? = null,
    large: Boolean = false,
) {
    val height = if (large) 72.dp else 60.dp
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("123", "456", "789").forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key -> Key(key.toString(), Modifier.weight(1f).height(height)) { onDigit(key) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(height), contentAlignment = Alignment.Center) { leftKey?.invoke() }
            Key("0", Modifier.weight(1f).height(height)) { onDigit('0') }
            Box(Modifier.weight(1f).height(height), contentAlignment = Alignment.Center) {
                IconButton(onClick = onDelete, modifier = Modifier.size(56.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Backspace, contentDescription = "Delete digit")
                }
            }
        }
    }
}

/** Shows key. */
@Composable
private fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium)
        }
    }
}

/** Shows otp field. */
@Composable
fun OtpField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier, length: Int = 6) {
    BasicTextField(
        value = value,
        onValueChange = { next -> if (next.length <= length && next.all { it.isDigit() }) onValueChange(next) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = modifier.semantics { contentDescription = "One-time code, $length digits" },
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val char = value.getOrNull(index)
                    val focused = index == value.length
                    Box(
                        Modifier.size(width = 44.dp, height = 56.dp)
                            .border(
                                if (focused) 2.dp else 1.dp,
                                if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(char?.toString() ?: "", style = MaterialTheme.typography.headlineSmall)
                    }
                }
            }
        },
    )
}

/** Shows text link. */
@Composable
fun TextLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier.padding(0.dp)) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@PreviewLightDark
@Composable
private fun SecurePreview() {
    PersonalLedgerTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AmountText(45_500L, MoneyDirection.DEBIT, label = "Team lunch")
            MaskedAmount(2_45_678_90L, revealed = false, onToggle = {})
            ProtectedBadge()
            PinDots(filled = 2, length = 4)
            OtpField("12", {})
            NumericKeypad(onDigit = {}, onDelete = {})
        }
    }
}
