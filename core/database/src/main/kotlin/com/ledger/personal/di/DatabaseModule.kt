package com.ledger.personal.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ledger.personal.data.local.LedgerDatabase
import com.ledger.personal.data.local.MIGRATION_1_2
import com.ledger.personal.data.local.MIGRATION_2_3
import com.ledger.personal.data.local.MIGRATION_3_4
import com.ledger.personal.data.local.MIGRATION_4_5
import com.ledger.personal.data.local.MIGRATION_5_6
import com.ledger.personal.data.local.PromotionDao
import com.ledger.personal.data.local.seedPromotions
import com.ledger.personal.data.security.AndroidKeyStoreMasterKeyVault
import com.ledger.personal.data.security.MasterKeyVault
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for database. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Supplies database. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LedgerDatabase {
        val builder = Room.databaseBuilder(context, LedgerDatabase::class.java, LedgerDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.seedPromotions()
                }
            })
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }
        return builder.build()
    }

    /** Supplies promotion dao. */
    @Provides
    fun providePromotionDao(database: LedgerDatabase): PromotionDao = database.promotionDao()

    /** Supplies master key vault for envelope encryption. */
    @Provides
    @Singleton
    fun provideMasterKeyVault(vault: AndroidKeyStoreMasterKeyVault): MasterKeyVault = vault
}

