package com.ledger.personal.data.local

import javax.inject.Inject
import javax.inject.Singleton

interface AssistantHistory {
    suspend fun add(token: String)
    suspend fun all(): List<String>
}

class MemoryAssistantHistory : AssistantHistory {
    private val tokens = linkedSetOf<String>()

    override suspend fun add(token: String) {
        tokens += token
    }

    override suspend fun all(): List<String> = tokens.toList()
}

@Singleton
class RoomAssistantHistory @Inject constructor(private val dao: AssistantDao) : AssistantHistory {
    override suspend fun add(token: String) {
        dao.insert(AssistantMessageEntity(token, dao.all().size))
    }

    override suspend fun all(): List<String> = dao.all().map { it.token }
}
