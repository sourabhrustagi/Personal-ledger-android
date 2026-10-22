package com.ledger.personal.core

import com.ledger.personal.domain.pay.BillPay
import com.ledger.personal.domain.pay.BillPayResponse
import com.ledger.personal.domain.pay.FetchedBill
import com.ledger.personal.domain.pay.SealedPay
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/** AES-GCM output. IV and body are different values, not a Pair. */
data class CipherText(val iv: ByteArray, val body: ByteArray)

/** ECDH P-256 plus AES-GCM. Both sides derive the same AES key from the two public keys. */
object PayBox {
    /** Fresh P-256 pair for one payment or for a mock server. */
    fun keyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        return generator.generateKeyPair()
    }

    /** Shared AES-256 key. ponytail: SHA-256 of the ECDH secret; HKDF if a second key is needed. */
    fun agree(mine: PrivateKey, peerPublic: ByteArray): ByteArray {
        val peer = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(peerPublic))
        val shared = KeyAgreement.getInstance("ECDH").run {
            init(mine)
            doPhase(peer, true)
            generateSecret()
        }
        return MessageDigest.getInstance("SHA-256").digest(shared)
    }

    /** AES-GCM seal. */
    fun seal(secret: ByteArray, plain: ByteArray): CipherText {
        val iv = ByteArray(IV_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secret, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return CipherText(iv, cipher.doFinal(plain))
    }

    /** AES-GCM open. */
    fun open(secret: ByteArray, iv: ByteArray, body: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secret, "AES"), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(body)
    }

    /** Packs a bill-number lookup. The number leaves the phone only inside the ciphertext. */
    fun packLookup(biller: String, billNumber: String): ByteArray =
        "lookup|$biller|$billNumber".encodeToByteArray()

    /** Packs the fields that leave the phone only inside the ciphertext. */
    fun pack(cardId: String, biller: String, amountMinor: Long, pin: String, billNumber: String = ""): ByteArray {
        val fields = "$cardId|$biller|$amountMinor|$pin"
        return (if (billNumber.isBlank()) fields else "$fields|$billNumber").encodeToByteArray()
    }

    private const val TRANSFORM = "AES/GCM/NoPadding"
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
}

/** Demo electricity bills the mock board will return. Any other number is not fetched. */
object ElectricityBills {
    val onFile: Map<String, Long> = mapOf(
        "104829" to 50_000L,
        "220184" to 125_000L,
    )
}

/** In-process server key. Mock flavor decrypts here; live decrypts on the host. */
class LocalBillPay : BillPay {
    private val pair = PayBox.keyPair()

    override suspend fun serverPublic(): ByteArray = pair.public.encoded

    override suspend fun fetch(sealed: SealedPay): Result<FetchedBill> = runCatching {
        val parts = open(sealed)
        require(parts.size == 3 && parts[0] == "lookup" && parts[1] == "electricity")
        val amount = ElectricityBills.onFile[parts[2]] ?: error("bill not on file")
        FetchedBill(parts[2], amount, "BESCOM")
    }

    override suspend fun submit(sealed: SealedPay): Result<BillPayResponse> = runCatching {
        val parts = open(sealed)
        require(parts.size in 4..5 && mpinAccepted(parts[3])) { "PIN rejected" }
        if (parts[1] == "electricity") {
            val due = ElectricityBills.onFile[parts.getOrNull(4).orEmpty()] ?: error("bill not on file")
            require(parts[2] == due.toString()) { "amount mismatch" }
        }
        BillPayResponse(status = "SUCCESS", message = "Bill paid successfully")
    }

    private fun open(sealed: SealedPay): List<String> {
        val secret = PayBox.agree(pair.private, sealed.clientPublic)
        return PayBox.open(secret, sealed.iv, sealed.body).decodeToString().split('|')
    }
}
