package com.ledger.personal.domain.servicing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionModelsTest {

    private val baseForm = InspectionForm(
        id = "insp-1",
        loanId = "loan-99",
        propertyAddress = "123 Elm St, Springfield",
        inspectorNotes = "Initial inspection draft",
        damageReport = "No visible foundation cracks",
        clientUpdatedAtEpochMs = 1_000L,
        baseServerVersion = 1L,
        syncStatus = "SYNCED",
    )

    @Test
    fun autoMergesDisjointEditsWithoutConflict() {
        // Local user edited notes; remote underwriter updated damage report
        val local = baseForm.copy(
            inspectorNotes = "Updated by field inspector offline",
            clientUpdatedAtEpochMs = 2_000L,
        )
        val server = baseForm.copy(
            damageReport = "Minor roof shingle wear observed by satellite",
            baseServerVersion = 2L,
        )

        val result = mergeInspectionForms(base = baseForm, local = local, server = server)

        assertFalse(result.hasConflicts)
        assertTrue(result.conflictingFields.isEmpty())
        assertEquals("Updated by field inspector offline", result.merged.inspectorNotes)
        assertEquals("Minor roof shingle wear observed by satellite", result.merged.damageReport)
        assertEquals("123 Elm St, Springfield", result.merged.propertyAddress)
        assertEquals(2L, result.merged.baseServerVersion)
        assertEquals("PENDING_UPDATE", result.merged.syncStatus)
    }

    @Test
    fun flagsConflictWhenSameFieldIsConcurrentlyModified() {
        val local = baseForm.copy(
            damageReport = "Severe water damage in basement",
            clientUpdatedAtEpochMs = 2_000L,
        )
        val server = baseForm.copy(
            damageReport = "Cosmetic dampness resolved",
            baseServerVersion = 2L,
        )

        val result = mergeInspectionForms(base = baseForm, local = local, server = server)

        assertTrue(result.hasConflicts)
        assertEquals(listOf("damageReport"), result.conflictingFields)
        assertEquals("CONFLICT", result.merged.syncStatus)
        // Retains local copy in draft while flagging conflict
        assertEquals("Severe water damage in basement", result.merged.damageReport)
    }
}
