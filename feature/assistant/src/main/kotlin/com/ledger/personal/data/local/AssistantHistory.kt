package com.ledger.personal.data.local

import javax.inject.Inject
import javax.inject.Singleton

/** Contract for assistant history. */
interface AssistantHistory {
    /** Add. */
    suspend fun add(token: String)
    /** All. */
    suspend fun all(): List<String>
}

/** Memory assistant history. */
class MemoryAssistantHistory : AssistantHistory {
    private val tokens = linkedSetOf<String>()

    /** Add. */
    override suspend fun add(token: String) {
        tokens += token
    }

    /** All. */
    override suspend fun all(): List<String> = tokens.toList()
}

/** Room assistant history. */
@Singleton
class RoomAssistantHistory @Inject constructor(private val dao: AssistantDao) : AssistantHistory {
    /** Add. */
    override suspend fun add(token: String) {
        dao.insert(AssistantMessageEntity(token, dao.all().size))
    }

    /** All. */
    override suspend fun all(): List<String> = dao.all().map { it.token }
}
