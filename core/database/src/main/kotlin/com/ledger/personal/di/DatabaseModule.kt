package com.ledger.personal.di

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.room.Room
import com.ledger.personal.data.local.AssistantDao
import com.ledger.personal.data.local.KycDraftDao
import com.ledger.personal.data.local.LedgerDatabase
import com.ledger.personal.data.local.MIGRATION_1_2
import com.ledger.personal.data.local.MIGRATION_2_3
import com.ledger.personal.data.local.PosForwardDao
import com.ledger.personal.data.local.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): LedgerDatabase {
        val builder = Room.databaseBuilder(context, LedgerDatabase::class.java, LedgerDatabase.NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (debuggable) {
            builder.fallbackToDestructiveMigration(dropAllTables = true)
        }
        return builder.build()
    }

    @Provides
    fun provideTransactionDao(database: LedgerDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideKycDraftDao(database: LedgerDatabase): KycDraftDao = database.kycDraftDao()

    @Provides
    fun provideAssistantDao(database: LedgerDatabase): AssistantDao = database.assistantDao()

    @Provides
    fun providePosForwardDao(database: LedgerDatabase): PosForwardDao = database.posForwardDao()
}
