package com.ledger.personal.domain.security

import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Domain model representing sensitive Mortgage PII data requiring PCI/PII grade protections.
 */
data class MortgagePiiRecord(
    val applicantId: String,
    val ssn: String,
    val taxReturnId: String,
    val bankAccountNumber: String,
    val propertyDeedId: String,
    val annualIncomePaise: Long,
)

/**
 * Encrypted Mortgage PII representation stored at rest.
 * Contains AES-GCM ciphertext, masked tokens for display, and HMAC blind indexes for search.
 */
data class EncryptedMortgagePii(
    val applicantId: String,
    val ciphertextBase64: String,
    val ivBase64: String,
    val maskedSsn: String,
    val maskedBankAccount: String,
    val ssnBlindIndex: String,
    val propertyDeedBlindIndex: String,
    val keyVersion: Int,
)

/**
 * Utilities for masking sensitive customer PII tokens for UI and logs.
 */
object PiiMasker {
    private const val SSN_VISIBLE_DIGITS = 4
    private const val ACCOUNT_VISIBLE_DIGITS = 4

    /** Masks a Social Security Number to '***-**-XXXX'. */
    fun maskSsn(ssn: String): String {
        val clean = ssn.filter { it.isDigit() }
        if (clean.length < SSN_VISIBLE_DIGITS) return "***-**-****"
        val last4 = clean.takeLast(SSN_VISIBLE_DIGITS)
        return "***-**-$last4"
    }

    /** Masks a bank account or financial identifier to '********XXXX'. */
    fun maskBankAccount(account: String): String {
        val clean = account.trim()
        if (clean.length <= ACCOUNT_VISIBLE_DIGITS) return "****"
        val maskedLength = clean.length - ACCOUNT_VISIBLE_DIGITS
        return "*".repeat(maskedLength) + clean.takeLast(ACCOUNT_VISIBLE_DIGITS)
    }

    /** Zeroizes sensitive character arrays in memory. */
    fun zeroize(chars: CharArray) {
        Arrays.fill(chars, '\u0000')
    }

    /** Zeroizes sensitive byte arrays in memory. */
    fun zeroize(bytes: ByteArray) {
        Arrays.fill(bytes, 0.toByte())
    }
}

/**
 * Cryptographic engine providing AES-256-GCM envelope encryption,
 * authenticated decryption, and HMAC-SHA256 blind indexing for sensitive mortgage records.
 */
object MortgagePiiCrypto {
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12
    private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val HMAC_ALGORITHM = "HmacSHA256"

    /**
     * Generates a cryptographically strong 256-bit AES key.
     */
    fun generateAes256Key(): ByteArray {
        val keyBytes = ByteArray(32)
        SecureRandom().nextBytes(keyBytes)
        return keyBytes
    }

    /**
     * Computes an HMAC-SHA256 blind index hash for querying encrypted data without revealing plaintext.
     */
    fun computeBlindIndex(value: String, blindIndexKey: ByteArray): String {
        val normalized = value.trim().lowercase()
        val mac = Mac.getInstance(HMAC_ALGORITHM)
        mac.init(SecretKeySpec(blindIndexKey, HMAC_ALGORITHM))
        val hash = mac.doFinal(normalized.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Encrypts a [MortgagePiiRecord] with AES-256-GCM.
     */
    fun encrypt(
        record: MortgagePiiRecord,
        dataEncryptionKey: ByteArray,
        blindIndexKey: ByteArray,
        keyVersion: Int = 1,
    ): EncryptedMortgagePii {
        require(dataEncryptionKey.size == 32) { "AES-256 key must be exactly 32 bytes" }
        require(blindIndexKey.size >= 16) { "Blind index key must be at least 16 bytes" }

        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val plaintext = serializeRecord(record)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(dataEncryptionKey, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        val ciphertext = cipher.doFinal(plaintext)

        return EncryptedMortgagePii(
            applicantId = record.applicantId,
            ciphertextBase64 = Base64.getEncoder().encodeToString(ciphertext),
            ivBase64 = Base64.getEncoder().encodeToString(iv),
            maskedSsn = PiiMasker.maskSsn(record.ssn),
            maskedBankAccount = PiiMasker.maskBankAccount(record.bankAccountNumber),
            ssnBlindIndex = computeBlindIndex(record.ssn, blindIndexKey),
            propertyDeedBlindIndex = computeBlindIndex(record.propertyDeedId, blindIndexKey),
            keyVersion = keyVersion,
        )
    }

    /**
     * Decrypts an [EncryptedMortgagePii] payload using AES-256-GCM.
     */
    fun decrypt(
        encrypted: EncryptedMortgagePii,
        dataEncryptionKey: ByteArray,
    ): MortgagePiiRecord {
        require(dataEncryptionKey.size == 32) { "AES-256 key must be exactly 32 bytes" }

        val iv = Base64.getDecoder().decode(encrypted.ivBase64)
        val ciphertext = Base64.getDecoder().decode(encrypted.ciphertextBase64)

        val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(dataEncryptionKey, "AES"),
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
        )
        val plaintext = cipher.doFinal(ciphertext)

        return deserializeRecord(encrypted.applicantId, plaintext)
    }

    private fun serializeRecord(record: MortgagePiiRecord): ByteArray {
        val payload = listOf(
            record.ssn,
            record.taxReturnId,
            record.bankAccountNumber,
            record.propertyDeedId,
            record.annualIncomePaise.toString(),
        ).joinToString("|")
        return payload.toByteArray(Charsets.UTF_8)
    }

    private fun deserializeRecord(applicantId: String, plaintextBytes: ByteArray): MortgagePiiRecord {
        val fields = plaintextBytes.toString(Charsets.UTF_8).split("|")
        require(fields.size == 5) { "Corrupt or invalid decrypted mortgage PII format" }
        return MortgagePiiRecord(
            applicantId = applicantId,
            ssn = fields[0],
            taxReturnId = fields[1],
            bankAccountNumber = fields[2],
            propertyDeedId = fields[3],
            annualIncomePaise = fields[4].toLongOrNull() ?: 0L,
        )
    }
}
