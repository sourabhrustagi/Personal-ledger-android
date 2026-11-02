package com.ledger.personal.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.ledger.personal.ui.theme.PersonalLedgerTheme
import com.ledger.personal.ui.theme.colorScheme

/**
 * Modern state holder illustrating the production replacement for the deprecated `@Model` annotation.
 *
 * In pre-1.0 Jetpack Compose, developers annotated classes with `@Model` to make mutable properties observable.
 * Because `@Model` relied on bytecode transformation magic, violated Unidirectional Data Flow (UDF), and didn't
 * survive process death, it was completely removed.
 *
 * This class demonstrates the idiomatic modern Jetpack Compose pattern:
 * 1. `@Stable` contract so Compose compiler knows properties notify reads/writes.
 * 2. Encapsulated `private set` so external composables cannot mutate internal state arbitrarily.
 * 3. Event-driven methods (`onAmountChange`, `onNoteChange`, `clear`) enforcing state consistency.
 * 4. Process-death survival through a custom [Saver].
 */
@Stable
class ModernInputState(
    initialAmount: String = "",
    initialNote: String = "",
) {
    var amountInput by mutableStateOf(initialAmount)
        private set

    var noteInput by mutableStateOf(initialNote)
        private set

    val amountMinor: Long
        get() = parseMinor(amountInput) ?: 0L

    val isValid: Boolean
        get() = amountMinor > 0L

    fun onAmountChange(value: String) {
        if (amountRegex.matches(value)) {
            amountInput = value
        }
    }

    fun onNoteChange(value: String) {
        if (value.length <= MAX_NOTE_LENGTH) {
            noteInput = value
        }
    }

    fun clear() {
        amountInput = ""
        noteInput = ""
    }

    companion object {
        private const val MAX_NOTE_LENGTH = 50
        private val amountRegex = Regex("^\\d{0,9}(\\.\\d{0,2})?$")

        val Saver: Saver<ModernInputState, Any> = listSaver(
            save = { listOf(it.amountInput, it.noteInput) },
            restore = { ModernInputState(it[0], it[1]) },
        )
    }
}

/**
 * Creates and remembers a [ModernInputState] surviving configuration changes and process recreation.
 */
@Composable
fun rememberModernInputState(
    initialAmount: String = "",
    initialNote: String = "",
): ModernInputState = rememberSaveable(saver = ModernInputState.Saver) {
    ModernInputState(initialAmount, initialNote)
}

/**
 * Example component demonstrating the modern state holder in action.
 */
@Composable
fun ModernStateHolderExample(
    state: ModernInputState = rememberModernInputState(),
    onConfirm: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    SectionCard(title = "Transfer Calculator", modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            AmountField(
                text = state.amountInput,
                onTextChange = state::onAmountChange,
                label = "Transfer Amount",
            )

            LedgerOneTextField(
                value = state.noteInput,
                onValueChange = state::onNoteChange,
                label = "Note (Optional)",
            )

            if (state.isValid) {
                LedgerOneText(
                    text = "Formatted: ${formatMoney(state.amountMinor)}",
                    role = LedgerOneTextRole.Body,
                    color = colorScheme.primary,
                    weight = FontWeight.Medium,
                )
            }

            LedgerOneButton(
                text = "Proceed Transfer",
                onClick = { onConfirm(state.amountMinor, state.noteInput) },
                enabled = state.isValid,
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ModernStateHolderExamplePreview() {
    PersonalLedgerTheme {
        val state = rememberModernInputState(initialAmount = "1500.50", initialNote = "Dinner bill")
        ModernStateHolderExample(state = state)
    }
}
