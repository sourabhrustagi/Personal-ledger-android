package com.ledger.personal.domain.pay

/** Ciphertext the phone sends after agreeing a key with the server. */
data class SealedPay(val clientPublic: ByteArray, val iv: ByteArray, val body: ByteArray)

/** Electricity bill returned after the board lookup is decrypted. */
data class FetchedBill(val billNumber: String, val amountMinor: Long, val holder: String)

/** Bill payment: the phone encrypts; the server decrypts with the agreed key. */
interface BillPay {
    /** Server EC public key (X.509). The AES key is never sent. */
    suspend fun serverPublic(): ByteArray

    /** Decrypts a sealed bill-number lookup. Fails when the board has no bill. */
    suspend fun fetch(sealed: SealedPay): Result<FetchedBill>

    /** Decrypts [sealed] with the key both sides derived from ECDH. */
    suspend fun submit(sealed: SealedPay): Result<BillPayResponse>
}
