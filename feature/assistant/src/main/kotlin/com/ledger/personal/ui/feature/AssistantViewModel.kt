package com.ledger.personal.ui.feature

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.core.ChargeQueue
import com.ledger.personal.data.local.AssistantHistory
import com.ledger.personal.data.local.MemoryAssistantHistory
import com.ledger.personal.domain.board.AssistantBackend
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** One stored turn before it is folded into the chat. */
private data class Entry(val role: ChatRole, val group: String, val id: String, val text: String)

/** State and actions for the assistant screen. */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val history: AssistantHistory,
    private val prompts: AssistantBackend,
) : ViewModel() {
    constructor(history: AssistantHistory) : this(history, QuietPrompts)
    constructor() : this(MemoryAssistantHistory(), QuietPrompts)

    private val queue = ChargeQueue()
    private val writes = Mutex()
    private val state = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = state.asStateFlow()

    init {
        viewModelScope.launch {
            prompts.prompts().fold(
                onSuccess = { list -> state.update { it.copy(prompts = list) } },
                onFailure = { state.update { it.copy(promptsMessage = "Suggestions couldn't load.") } },
            )
        }
        viewModelScope.launch {
            val saved = history.all().map(::decode)
            if (saved.isEmpty()) return@launch
            queue.replay(saved.map { it.id })
            state.update { current ->
                if (current.messages.isNotEmpty()) current else saved.fold(current) { acc, entry -> acc.with(entry) }
            }
        }
    }

    /** Consume. */
    fun consume(line: String) {
        val token = line.removePrefix("data:").trim()
        if (token.isEmpty() || token == "[DONE]") return
        push(token)
    }

    /** Push. */
    fun push(token: String) {
        append(Entry(ChatRole.ASSISTANT, STREAM, token, token))
    }

    /** Ask. */
    fun ask(question: String) {
        val text = question.trim()
        if (text.isEmpty() || state.value.isTyping) return
        val turn = System.currentTimeMillis().toString()
        append(Entry(ChatRole.USER, "q$turn", "q$turn", text))
        viewModelScope.launch {
            state.update { it.copy(isTyping = true) }
            delay(THINKING_MS)
            ledgerAnswer(text).split(" ").forEachIndexed { index, word ->
                append(Entry(ChatRole.ASSISTANT, "a$turn", "a$turn-$index", if (index == 0) word else " $word"))
                delay(WORD_MS)
            }
            state.update { it.copy(isTyping = false) }
        }
    }

    /** Append. */
    private fun append(entry: Entry) {
        val before = queue.charges
        queue.replay(listOf(entry.id))
        if (queue.charges == before) return
        viewModelScope.launch { writes.withLock { history.add(encode(entry)) } }
        state.update { it.with(entry) }
    }

    /** With. */
    private fun AssistantUiState.with(entry: Entry): AssistantUiState {
        val last = messages.lastOrNull()
        val merged = if (entry.role == ChatRole.ASSISTANT && last?.role == ChatRole.ASSISTANT && last.group == entry.group) {
            messages.dropLast(1) + last.copy(text = last.text + entry.text)
        } else {
            messages + ChatMessage(entry.group, entry.role, entry.text)
        }
        return copy(
            tokens = if (entry.role == ChatRole.ASSISTANT) tokens + entry.text else tokens,
            messages = merged,
            isEmpty = false,
            isLoading = false,
        )
    }

    /** Encode. */
    private fun encode(entry: Entry): String =
        if (entry.group == STREAM && entry.id == entry.text) entry.text else listOf(entry.role.name, entry.group, entry.id, entry.text).joinToString(SEP)

    /** Decode. */
    private fun decode(raw: String): Entry {
        val parts = raw.split(SEP, limit = 4)
        return if (parts.size == 4) Entry(ChatRole.valueOf(parts[0]), parts[1], parts[2], parts[3]) else Entry(ChatRole.ASSISTANT, STREAM, raw, raw)
    }

    private companion object {
        val QuietPrompts = object : AssistantBackend {
            override suspend fun prompts() = Result.success(emptyList<String>())
        }
        const val STREAM = "stream"
        const val SEP = "\u001F"
        const val THINKING_MS = 400L
        const val WORD_MS = 35L
    }
}
