package com.ledger.personal.data.local

import javax.inject.Inject
import javax.inject.Singleton

interface PosForwardStore {
    suspend fun enqueue(key: String, amountMinor: Long)
    suspend fun count(): Int

    /** Signed amounts not yet settled; refunds are stored as negative rows. */
    suspend fun unsettled(): List<Long>

    /** Marks every unsettled row settled and returns how many were closed. */
    suspend fun settleAll(): Int
}

class MemoryPosForward : PosForwardStore {
    private val rows = linkedMapOf<String, Long>()
    private val settled = mutableSetOf<String>()

    override suspend fun enqueue(key: String, amountMinor: Long) {
        rows.putIfAbsent(key, amountMinor)
    }

    override suspend fun count(): Int = rows.size

    override suspend fun unsettled(): List<Long> = rows.filterKeys { it !in settled }.values.toList()

    override suspend fun settleAll(): Int {
        val open = rows.keys - settled
        settled += open
        return open.size
    }
}

@Singleton
class RoomPosForward @Inject constructor(private val dao: PosForwardDao) : PosForwardStore {
    override suspend fun enqueue(key: String, amountMinor: Long) {
        dao.enqueue(PosForwardEntity(key, amountMinor, settled = 0))
    }

    override suspend fun count(): Int = dao.count()

    override suspend fun unsettled(): List<Long> = dao.unsettled().map { it.amountMinor }

    override suspend fun settleAll(): Int = dao.settleAll()
}

interface CardReader {
    suspend fun readLastFour(): String
}

class FakeCardReader : CardReader {
    override suspend fun readLastFour(): String = "1111"
}

interface ReceiptPrinter {
    suspend fun print(receipt: String)
}

class FakeReceiptPrinter : ReceiptPrinter {
    override suspend fun print(receipt: String) = Unit
}
