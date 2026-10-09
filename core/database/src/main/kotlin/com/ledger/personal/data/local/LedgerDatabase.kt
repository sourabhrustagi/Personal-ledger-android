package com.ledger.personal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        TransactionEntity::class,
        KycDraftEntity::class,
        AssistantMessageEntity::class,
        PosForwardEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
@TypeConverters(MoneyConverter::class)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun kycDraftDao(): KycDraftDao
    abstract fun assistantDao(): AssistantDao
    abstract fun posForwardDao(): PosForwardDao

    companion object {
        const val NAME = "ledger.db"
    }
}
