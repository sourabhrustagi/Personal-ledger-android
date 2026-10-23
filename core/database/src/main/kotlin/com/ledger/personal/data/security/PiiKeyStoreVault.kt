package com.ledger.personal.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Envelope encryption vault providing Key Encryption Key (KEK) management
 * via Android Keystore and wrapping/unwrapping Database Encryption Keys (DEK)
 * for SQLCipher and encrypted storage.
 */
interface MasterKeyVault {
    /** Wraps a 256-bit Data Encryption Key (DEK) using the Keystore Master Key. */
    fun wrapDek(dek: ByteArray): String

    /** Unwraps an encrypted DEK payload using the Keystore Master Key. */
    fun unwrapDek(wrappedDekBase64: String): ByteArray
}

/**
 * Hardware-backed Android Keystore Master Key Vault implementation.
 */
@Singleton
class AndroidKeyStoreMasterKeyVault @Inject constructor() : MasterKeyVault {

    private val store by lazy { KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) } }

    override fun wrapDek(dek: ByteArray): String {
        require(dek.size == 32) { "DEK must be exactly 32 bytes (256-bit)" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(dek)
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun unwrapDek(wrappedDekBase64: String): ByteArray {
        val payload = Base64.getDecoder().decode(wrappedDekBase64)
        require(payload.size > IV_SIZE_BYTES) { "Invalid wrapped DEK payload size" }

        val iv = payload.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = payload.copyOfRange(IV_SIZE_BYTES, payload.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateMasterKey(): SecretKey {
        return (store.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)
            ?: run {
                val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                generator.init(
                    KeyGenParameterSpec.Builder(
                        MASTER_KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generator.generateKey()
            }
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "ledgerone_master_kek"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE_BYTES = 12
        private const val TAG_LENGTH_BITS = 128
    }
}

/**
 * Software-backed Master Key Vault for JVM unit testing environments.
 */
class SoftwareMasterKeyVault(
    private val masterKeyBytes: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) },
) : MasterKeyVault {

    private val masterKey: SecretKey = SecretKeySpec(masterKeyBytes, "AES")

    override fun wrapDek(dek: ByteArray): String {
        require(dek.size == 32) { "DEK must be exactly 32 bytes (256-bit)" }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(dek)
        return Base64.getEncoder().encodeToString(iv + encrypted)
    }

    override fun unwrapDek(wrappedDekBase64: String): ByteArray {
        val payload = Base64.getDecoder().decode(wrappedDekBase64)
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, masterKey, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }
}
