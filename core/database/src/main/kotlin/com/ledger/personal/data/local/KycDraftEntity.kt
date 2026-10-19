package com.ledger.personal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Database row for kyc draft. */
@Entity(tableName = "kyc_drafts")
data class KycDraftEntity(
    @PrimaryKey val id: String,
    val name: String,
    val confidence: Double,
    val captureUri: String?,
)
