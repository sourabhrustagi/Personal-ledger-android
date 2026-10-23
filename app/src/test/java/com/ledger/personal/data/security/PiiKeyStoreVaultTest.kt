package com.ledger.personal.data.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class PiiKeyStoreVaultTest {

    private val vault = SoftwareMasterKeyVault()

    @Test
    fun wrapAndUnwrapDekRoundTripSucceeds() {
        val originalDek = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrappedDek = vault.wrapDek(originalDek)

        val unwrappedDek = vault.unwrapDek(wrappedDek)

        assertArrayEquals(originalDek, unwrappedDek)
    }

    @Test
    fun multipleWrapsProduceDistinctCiphertextsDueToFreshIv() {
        val originalDek = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrap1 = vault.wrapDek(originalDek)
        val wrap2 = vault.wrapDek(originalDek)

        assertNotEquals(wrap1, wrap2)
        assertArrayEquals(vault.unwrapDek(wrap1), vault.unwrapDek(wrap2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun throwsWhenDekIsNot256Bits() {
        val invalidDek = ByteArray(16)
        vault.wrapDek(invalidDek)
    }

    @Test
    fun tamperingWrappedDekFailsAuthentication() {
        val originalDek = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val wrappedDek = vault.wrapDek(originalDek)
        val tampered = wrappedDek.dropLast(4) + "AAAA"

        val result = runCatching { vault.unwrapDek(tampered) }
        assertTrue(result.isFailure)
    }
}
