package com.ledger.personal.domain.kyc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CaseTransitionTest {

    @Test
    fun rejectsNewToApproved() {
        val open = KycCase(id = "case-1", status = CaseStatus.NEW)

        val exception = assertThrows(IllegalStateException::class.java) {
            open.transitionTo(CaseStatus.APPROVED)
        }

        assertTrue(exception.message.orEmpty().contains("NEW"))
        assertTrue(exception.message.orEmpty().contains("APPROVED"))
        assertEquals(CaseStatus.NEW, open.status)
    }
}
