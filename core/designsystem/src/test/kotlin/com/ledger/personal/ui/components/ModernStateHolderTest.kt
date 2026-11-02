package com.ledger.personal.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModernStateHolderTest {

    @Test
    fun defaultStateInitializesEmpty() {
        val state = ModernInputState()
        assertEquals("", state.amountInput)
        assertEquals("", state.noteInput)
        assertEquals(0L, state.amountMinor)
        assertFalse(state.isValid)
    }

    @Test
    fun customStateInitializesWithValues() {
        val state = ModernInputState(initialAmount = "150.75", initialNote = "Groceries")
        assertEquals("150.75", state.amountInput)
        assertEquals("Groceries", state.noteInput)
        assertEquals(15075L, state.amountMinor)
        assertTrue(state.isValid)
    }

    @Test
    fun validatesAndRejectsMalformedAmountInput() {
        val state = ModernInputState()
        state.onAmountChange("100.50")
        assertEquals("100.50", state.amountInput)
        assertEquals(10050L, state.amountMinor)
        assertTrue(state.isValid)

        // Rejects extra decimal points
        state.onAmountChange("100.50.2")
        assertEquals("100.50", state.amountInput)

        // Rejects 3 decimal places
        state.onAmountChange("100.555")
        assertEquals("100.50", state.amountInput)
    }

    @Test
    fun rejectsNotesExceedingMaxLength() {
        val state = ModernInputState()
        val validNote = "A".repeat(50)
        state.onNoteChange(validNote)
        assertEquals(validNote, state.noteInput)

        val tooLongNote = "A".repeat(51)
        state.onNoteChange(tooLongNote)
        assertEquals(validNote, state.noteInput)
    }

    @Test
    fun clearResetsAllFields() {
        val state = ModernInputState(initialAmount = "500", initialNote = "Test")
        assertTrue(state.isValid)
        state.clear()
        assertEquals("", state.amountInput)
        assertEquals("", state.noteInput)
        assertEquals(0L, state.amountMinor)
        assertFalse(state.isValid)
    }

    @Test
    fun saverPreservesStateAcrossRecreation() {
        val original = ModernInputState(initialAmount = "250.00", initialNote = "Lunch with team")
        val saved = with(ModernInputState.Saver) {
            mockSaverScope.save(original)
        }
        requireNotNull(saved)

        val restored = ModernInputState.Saver.restore(saved)
        requireNotNull(restored)
        assertEquals(original.amountInput, restored.amountInput)
        assertEquals(original.noteInput, restored.noteInput)
        assertEquals(original.amountMinor, restored.amountMinor)
        assertEquals(original.isValid, restored.isValid)
    }

    private companion object {
        val mockSaverScope = object : androidx.compose.runtime.saveable.SaverScope {
            override fun canBeSaved(value: Any): Boolean = true
        }
    }
}
