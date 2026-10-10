package com.ledger.personal.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec

/** OWASP 2023 minimum for PBKDF2-HMAC-SHA256. Checked off the main thread. */
const val PIN_ITERATIONS = 600_000
const val PIN_V2 = "v2:"

/** Derives pin. */
fun derivePin(pin: String, salt: ByteArray): ByteArray = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    .generateSecret(PBEKeySpec(pin.toCharArray(), salt, PIN_ITERATIONS, 256))
    .encoded

/** Legacy pin hash. */
fun legacyPinHash(pin: String, salt: String): String =
    MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).joinToString("") { "%02x".format(it) }

/** Legacy matches. */
fun legacyMatches(pin: String, salt: String, hash: String): Boolean =
    MessageDigest.isEqual(legacyPinHash(pin, salt).toByteArray(), hash.toByteArray())

/** Fresh salt plus a Keystore-sealed verifier. The DataStore file alone cannot be checked offline. */
fun sealPin(pin: String): Pair<String, String> {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val hex = salt.joinToString("") { "%02x".format(it) }
    return hex to (PIN_V2 + PinSealer.seal(derivePin(pin, salt)))
}

/** Pin matches. */
fun pinMatches(pin: String, saltHex: String, stored: String): Boolean = if (stored.startsWith(PIN_V2)) {
    val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    MessageDigest.isEqual(derivePin(pin, salt), PinSealer.open(stored.removePrefix(PIN_V2)))
} else {
    legacyMatches(pin, saltHex, stored)
}

/** AES-GCM key that never leaves the Android Keystore. */
object PinSealer {
    /** Seal. */
    fun seal(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val payload = cipher.iv + cipher.doFinal(plain)
        return Base64.getEncoder().encodeToString(payload)
    }

    /** Open. */
    fun open(sealed: String): ByteArray {
        val payload = Base64.getDecoder().decode(sealed)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, payload.copyOfRange(0, IV_BYTES)))
        return cipher.doFinal(payload.copyOfRange(IV_BYTES, payload.size))
    }

    /** Delete. */
    fun delete() {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        if (store.containsAlias(ALIAS)) store.deleteEntry(ALIAS)
    }

    /** Key. */
    private fun key(): SecretKey {
        val store = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private const val KEYSTORE = "AndroidKeyStore"
    private const val ALIAS = "ledgerone_pin"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
}
