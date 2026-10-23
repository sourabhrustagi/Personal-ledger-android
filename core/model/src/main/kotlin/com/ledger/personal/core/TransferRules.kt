package com.ledger.personal.core

import java.util.concurrent.ConcurrentHashMap

/** Records a transfer once per idempotency key. */
class TransferBook {
    private val posted = ConcurrentHashMap<String, Long>()

    /** Stores the amount the first time this key is seen and returns that same amount afterwards. */
    fun submit(idempotencyKey: String, amountMinor: Long): Long = posted.computeIfAbsent(idempotencyKey) { amountMinor }

    /** Returns the posted amount if this idempotency key was previously processed, or null if unrecorded. */
    fun status(idempotencyKey: String): Long? = posted[idempotencyKey]

    /** How many distinct transfers have been recorded. */
    val size: Int get() = posted.size

    /** Sum of every distinct transfer. */
    val total: Long get() = posted.values.sum()
}

/** Result of checking whether the actor may see a wallet. */
sealed interface WalletLookup {
    /** The actor is allowed to see this wallet. */
    data class Found(val owner: String) : WalletLookup
    /** The actor is not the owner, so the wallet is hidden. */
    data object NotFound : WalletLookup
}

/** Returns the wallet only when the actor is the owner. */
fun lookupWallet(actor: String, owner: String): WalletLookup =
    if (actor == owner) WalletLookup.Found(owner) else WalletLookup.NotFound

/** True when the PIN is exactly four digits. */
fun mpinAccepted(pin: String): Boolean = pin.length == 4 && pin.all { it.isDigit() }

/** Builds a UPI pay link for a VPA and an amount in paise. */
fun upiPayload(vpa: String, amountMinor: Long): String {
    val rupees = amountMinor / 100
    val paise = (amountMinor % 100).toString().padStart(2, '0')
    return "upi://pay?pa=$vpa&am=$rupees.$paise"
}

/** True while a new payee is still inside the 30 minute cooling period. */
fun coolingBlocks(addedAtEpochMs: Long, nowEpochMs: Long): Boolean = nowEpochMs - addedAtEpochMs < 30 * 60 * 1000L
