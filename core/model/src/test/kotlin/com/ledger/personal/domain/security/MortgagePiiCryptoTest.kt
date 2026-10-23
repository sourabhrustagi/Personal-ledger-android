package com.ledger.personal.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MortgagePiiCryptoTest {

    private val record = MortgagePiiRecord(
        applicantId = "APP-9021",
        ssn = "123-45-6789",
        taxReturnId = "W2-2025-CA-4411",
        bankAccountNumber = "987654321098",
        propertyDeedId = "DEED-SF-88120",
        annualIncomePaise = 15_000_000_00L,
    )

    private val dek = MortgagePiiCrypto.generateAes256Key()
    private val blindKey = MortgagePiiCrypto.generateAes256Key()

    @Test
    fun encryptAndDecryptRoundTripMatchesOriginal() {
        val encrypted = MortgagePiiCrypto.encrypt(record, dek, blindKey, keyVersion = 1)
        val decrypted = MortgagePiiCrypto.decrypt(encrypted, dek)

        assertEquals(record.applicantId, decrypted.applicantId)
        assertEquals(record.ssn, decrypted.ssn)
        assertEquals(record.taxReturnId, decrypted.taxReturnId)
        assertEquals(record.bankAccountNumber, decrypted.bankAccountNumber)
        assertEquals(record.propertyDeedId, decrypted.propertyDeedId)
        assertEquals(record.annualIncomePaise, decrypted.annualIncomePaise)
    }

    @Test
    fun maskingProtectsDisplayAndLogs() {
        assertEquals("***-**-6789", PiiMasker.maskSsn("123-45-6789"))
        assertEquals("********1098", PiiMasker.maskBankAccount("987654321098"))
    }

    @Test
    fun blindIndexMatchesDeterministicLookupWithoutPlaintext() {
        val encrypted = MortgagePiiCrypto.encrypt(record, dek, blindKey)
        val queryHash = MortgagePiiCrypto.computeBlindIndex("123-45-6789", blindKey)

        assertEquals(encrypted.ssnBlindIndex, queryHash)

        val mismatchedQuery = MortgagePiiCrypto.computeBlindIndex("999-99-9999", blindKey)
        assertNotEquals(encrypted.ssnBlindIndex, mismatchedQuery)
    }

    @Test
    fun tamperingCiphertextFailsAuthenticationTag() {
        val encrypted = MortgagePiiCrypto.encrypt(record, dek, blindKey)
        val tamperedCiphertext = encrypted.ciphertextBase64.dropLast(4) + "AAAA"
        val tampered = encrypted.copy(ciphertextBase64 = tamperedCiphertext)

        val result = runCatching { MortgagePiiCrypto.decrypt(tampered, dek) }
        assertTrue(result.isFailure)
    }

    @Test
    fun zeroizeClearsMemoryBuffers() {
        val sensitiveChars = charArrayOf('1', '2', '3', '4', '5')
        PiiMasker.zeroize(sensitiveChars)
        assertTrue(sensitiveChars.all { it == '\u0000' })

        val sensitiveBytes = byteArrayOf(1, 2, 3, 4, 5)
        PiiMasker.zeroize(sensitiveBytes)
        assertTrue(sensitiveBytes.all { it == 0.toByte() })
    }
}
