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
        PromotionEntity::class,
        InspectionFormEntity::class,
        InspectionPhotoEntity::class,
    ],
    version = 5,
    exportSchema = true,
)
/** Ledger database. */
@TypeConverters(MoneyConverter::class)
abstract class LedgerDatabase : RoomDatabase() {
    /** Transaction dao. */
    abstract fun transactionDao(): TransactionDao
    /** Kyc draft dao. */
    abstract fun kycDraftDao(): KycDraftDao
    /** Assistant dao. */
    abstract fun assistantDao(): AssistantDao
    /** Pos forward dao. */
    abstract fun posForwardDao(): PosForwardDao
    /** Promotion dao. */
    abstract fun promotionDao(): PromotionDao
    /** Inspection dao. */
    abstract fun inspectionDao(): InspectionDao

    companion object {
        const val NAME = "ledger.db"
    }
}
