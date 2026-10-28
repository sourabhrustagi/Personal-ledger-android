package com.ledger.personal.feature.ledger.demo

import android.content.Context
import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.ConflictInbox
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LedgerDemoModule {

    @Provides
    @Singleton
    fun provideTransactionRemote(mock: MockTransactionRepository): TransactionRemote = mock

    @Provides
    @Singleton
    fun provideConflictInbox(@ApplicationContext context: Context): ConflictInbox =
        ConflictInbox(File(context.filesDir, "sync-conflicts.txt"))
}
