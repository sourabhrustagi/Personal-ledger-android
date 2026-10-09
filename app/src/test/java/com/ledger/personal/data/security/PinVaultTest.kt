package com.ledger.personal.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

class PinVaultTest {
    @Test
    fun legacyHashStillMatchesAndADifferentPinDoesNot() {
        val hash = legacyPinHash("2580", "abcd")
        assertTrue(legacyMatches("2580", "abcd", hash))
        assertFalse(legacyMatches("0000", "abcd", hash))
    }

    @Test
    fun derivedPinIsStableForTheSameSalt() {
        val salt = ByteArray(16) { it.toByte() }
        val first = derivePin("2580", salt)
        assertTrue(MessageDigest.isEqual(first, derivePin("2580", salt)))
        assertFalse(MessageDigest.isEqual(first, derivePin("2581", salt)))
    }
}
