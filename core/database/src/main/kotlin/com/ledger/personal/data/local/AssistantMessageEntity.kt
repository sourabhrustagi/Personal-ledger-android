package com.ledger.personal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Database row for assistant message. */
@Entity(tableName = "assistant_messages")
data class AssistantMessageEntity(
    @PrimaryKey val token: String,
    val position: Int,
)
