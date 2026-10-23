package com.ledger.personal.domain.servicing

/**
 * Domain model representing a multi-page mortgage property inspection / loan modification form.
 * Designed for offline-first usage with optimistic concurrency control (baseServerVersion).
 */
data class InspectionForm(
    val id: String,
    val loanId: String,
    val propertyAddress: String,
    val inspectorNotes: String,
    val damageReport: String,
    val clientUpdatedAtEpochMs: Long,
    val baseServerVersion: Long,
    val syncStatus: String,
)

/**
 * Domain model representing a property inspection photo attachment.
 * Supports resumable chunked uploading with byte-level offset tracking to survive network drop-offs.
 */
data class InspectionPhoto(
    val photoId: String,
    val formId: String,
    val localFilePath: String,
    val fileSizeBytes: Long,
    val bytesUploaded: Long,
    val checksum: String,
    val remoteUrl: String?,
    val uploadStatus: String,
)

/** Allowed upload statuses for large media attachments. */
object MediaUploadStatus {
    const val QUEUED: String = "QUEUED"
    const val COMPRESSING: String = "COMPRESSING"
    const val UPLOADING: String = "UPLOADING"
    const val COMPLETED: String = "COMPLETED"
    const val FAILED: String = "FAILED"
}

/** Result of a 3-way merge between base, local, and server copies of an inspection form. */
data class FormMergeResult(
    val merged: InspectionForm,
    val hasConflicts: Boolean,
    val conflictingFields: List<String>,
)

/**
 * 3-way merge algorithm resolving concurrent modifications:
 * - Non-overlapping field updates are automatically merged.
 * - Conflicting overlapping updates are detected and flagged for user resolution.
 */
fun mergeInspectionForms(
    base: InspectionForm,
    local: InspectionForm,
    server: InspectionForm,
): FormMergeResult {
    val conflictingFields = mutableListOf<String>()

    val propertyAddress = when {
        local.propertyAddress == server.propertyAddress -> local.propertyAddress
        local.propertyAddress != base.propertyAddress && server.propertyAddress == base.propertyAddress -> local.propertyAddress
        local.propertyAddress == base.propertyAddress && server.propertyAddress != base.propertyAddress -> server.propertyAddress
        else -> {
            conflictingFields.add("propertyAddress")
            local.propertyAddress
        }
    }

    val inspectorNotes = when {
        local.inspectorNotes == server.inspectorNotes -> local.inspectorNotes
        local.inspectorNotes != base.inspectorNotes && server.inspectorNotes == base.inspectorNotes -> local.inspectorNotes
        local.inspectorNotes == base.inspectorNotes && server.inspectorNotes != base.inspectorNotes -> server.inspectorNotes
        else -> {
            conflictingFields.add("inspectorNotes")
            local.inspectorNotes
        }
    }

    val damageReport = when {
        local.damageReport == server.damageReport -> local.damageReport
        local.damageReport != base.damageReport && server.damageReport == base.damageReport -> local.damageReport
        local.damageReport == base.damageReport && server.damageReport != base.damageReport -> server.damageReport
        else -> {
            conflictingFields.add("damageReport")
            local.damageReport
        }
    }

    val hasConflicts = conflictingFields.isNotEmpty()
    val mergedForm = local.copy(
        propertyAddress = propertyAddress,
        inspectorNotes = inspectorNotes,
        damageReport = damageReport,
        baseServerVersion = maxOf(local.baseServerVersion, server.baseServerVersion),
        syncStatus = if (hasConflicts) "CONFLICT" else "PENDING_UPDATE",
    )

    return FormMergeResult(
        merged = mergedForm,
        hasConflicts = hasConflicts,
        conflictingFields = conflictingFields,
    )
}
