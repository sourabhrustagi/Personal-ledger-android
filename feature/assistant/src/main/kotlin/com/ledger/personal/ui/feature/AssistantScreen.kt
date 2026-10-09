package com.ledger.personal.ui.feature

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AssistantScreen(
    onBack: (() -> Unit)?,
    onReviewPayment: () -> Unit = {},
    offline: Boolean = false,
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var draft by rememberSaveable { mutableStateOf("") }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.messages.lastOrNull()?.text?.length) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }
    val send = {
        val intent = paymentIntent(draft)
        if (intent != null) pending = intent else viewModel.ask(draft)
        draft = ""
    }
    FeatureScaffold(
        "Assistant",
        onBack,
        subtitle = if (state.isTyping) "Typing…" else "Answers from your ledger",
        scrollable = false,
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Ask about your money") },
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(onClick = send, enabled = draft.isNotBlank() && !state.isTyping) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        },
    ) {
        if (offline) StatusBanner("You're offline. Answers use the copy of your ledger saved on this phone.", BannerTone.WARNING)
        pending?.let { summary ->
            ConfirmCard(summary, onConfirm = {
                pending = null
                onReviewPayment()
            }, onCancel = { pending = null })
        }
        if (state.messages.isEmpty()) {
            Welcome(onPick = viewModel::ask)
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(state.messages) { message -> Bubble(message) }
                if (state.isTyping && state.messages.last().role == ChatRole.USER) {
                    item { Bubble(ChatMessage("typing", ChatRole.ASSISTANT, "…")) }
                }
            }
        }
    }
}

@Composable
private fun Welcome(onPick: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBadge(Icons.Outlined.AutoAwesome, size = 72)
        Text("Hi! Ask me about your money", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "I read your ledger on this device to answer. Nothing you ask leaves your phone, and I never move money without your confirmation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    assistantSuggestions.forEach { suggestion ->
        SuggestionChip(onClick = { onPick(suggestion) }, label = { Text(suggestion) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Bubble(message: ChatMessage) {
    val mine = message.role == ChatRole.USER
    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (mine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (mine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mine) 16.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 16.dp,
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(message.text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
        }
    }
}

private val payRegex = Regex("""(?i)\b(send|pay|transfer)\b\s*(?:rs\.?|₹)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:to\s+(.+))?""")

fun paymentIntent(text: String): String? {
    val match = payRegex.find(text) ?: return null
    val minor = parseMinor(match.groupValues[2].replace(",", "")) ?: return null
    val who = match.groupValues[3].trim().ifBlank { "a contact" }
    return "Send ${formatMoney(minor)} to ${who.replaceFirstChar { it.uppercase() }}"
}

@Composable
private fun ConfirmCard(summary: String, onConfirm: () -> Unit, onCancel: () -> Unit) {
    SectionCard(title = "Confirm before I proceed") {
        Text(summary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "You'll review the payee and amount, then approve with biometrics or MPIN.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            androidx.compose.material3.Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Review in Pay") }
        }
    }
}
