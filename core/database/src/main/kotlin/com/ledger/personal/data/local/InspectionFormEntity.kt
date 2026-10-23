package com.ledger.personal.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ledger.personal.domain.servicing.InspectionForm

/** Database row for offline mortgage property inspection / loan modification form. */
@Entity(tableName = "inspection_forms")
data class InspectionFormEntity(
    @PrimaryKey val id: String,
    val loanId: String,
    val propertyAddress: String,
    val inspectorNotes: String,
    val damageReport: String,
    val clientUpdatedAtEpochMs: Long,
    val baseServerVersion: Long,
    val syncStatus: String,
)

fun InspectionFormEntity.toDomain(): InspectionForm = InspectionForm(
    id = id,
    loanId = loanId,
    propertyAddress = propertyAddress,
    inspectorNotes = inspectorNotes,
    damageReport = damageReport,
    clientUpdatedAtEpochMs = clientUpdatedAtEpochMs,
    baseServerVersion = baseServerVersion,
    syncStatus = syncStatus,
)

fun InspectionForm.toEntity(): InspectionFormEntity = InspectionFormEntity(
    id = id,
    loanId = loanId,
    propertyAddress = propertyAddress,
    inspectorNotes = inspectorNotes,
    damageReport = damageReport,
    clientUpdatedAtEpochMs = clientUpdatedAtEpochMs,
    baseServerVersion = baseServerVersion,
    syncStatus = syncStatus,
)
