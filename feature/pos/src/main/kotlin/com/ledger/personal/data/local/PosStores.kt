package com.ledger.personal.data.local

import javax.inject.Inject
import javax.inject.Singleton

/** Contract for pos forward store. */
interface PosForwardStore {
    /** Enqueue. */
    suspend fun enqueue(key: String, amountMinor: Long)
    /** Count. */
    suspend fun count(): Int

    /** Signed amounts not yet settled; refunds are stored as negative rows. */
    suspend fun unsettled(): List<Long>

    /** Marks every unsettled row settled and returns how many were closed. */
    suspend fun settleAll(): Int
}

/** Memory pos forward. */
class MemoryPosForward : PosForwardStore {
    private val rows = linkedMapOf<String, Long>()
    private val settled = mutableSetOf<String>()

    /** Enqueue. */
    override suspend fun enqueue(key: String, amountMinor: Long) {
        rows.putIfAbsent(key, amountMinor)
    }

    /** Count. */
    override suspend fun count(): Int = rows.size

    /** Unsettled. */
    override suspend fun unsettled(): List<Long> = rows.filterKeys { it !in settled }.values.toList()

    /** Settles all. */
    override suspend fun settleAll(): Int {
        val open = rows.keys - settled
        settled += open
        return open.size
    }
}

/** Room pos forward. */
@Singleton
class RoomPosForward @Inject constructor(private val dao: PosForwardDao) : PosForwardStore {
    /** Enqueue. */
    override suspend fun enqueue(key: String, amountMinor: Long) {
        dao.enqueue(PosForwardEntity(key, amountMinor, settled = 0))
    }

    /** Count. */
    override suspend fun count(): Int = dao.count()

    /** Unsettled. */
    override suspend fun unsettled(): List<Long> = dao.unsettled().map { it.amountMinor }

    /** Settles all. */
    override suspend fun settleAll(): Int = dao.settleAll()
}

/** Contract for card reader. */
interface CardReader {
    /** Read last four. */
    suspend fun readLastFour(): String
}

/** Mock card reader. */
class MockCardReader : CardReader {
    /** Read last four. */
    override suspend fun readLastFour(): String = "1111"
}

/** Contract for receipt printer. */
interface ReceiptPrinter {
    /** Print. */
    suspend fun print(receipt: String)
}

/** Mock receipt printer. */
class MockReceiptPrinter : ReceiptPrinter {
    /** Print. */
    override suspend fun print(receipt: String) = Unit
}
