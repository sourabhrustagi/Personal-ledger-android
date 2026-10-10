package com.ledger.personal.ui.feature

/** Allowed values of chat role. */
enum class ChatRole { USER, ASSISTANT }

/** Chat message. */
data class ChatMessage(val group: String, val role: ChatRole, val text: String)

/** Values rendered by the assistant screen. */
data class AssistantUiState(
    val isLoading: Boolean = false,
    val tokens: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val isEmpty: Boolean = true,
    val errorMessage: String? = null,
    val prompts: List<String> = emptyList(),
    val promptsMessage: String? = null,
)
