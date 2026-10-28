package com.ledger.personal.feature.ledger.demo

import android.content.Context
import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.mock.MockHomeBoard
import com.ledger.personal.mock.MockPromotionsBackend
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LedgerDemoModule {

    @Binds
    @Singleton
    abstract fun bindHomeBoard(mock: MockHomeBoard): HomeBoard

    @Binds
    @Singleton
    abstract fun bindPromotionsBackend(mock: MockPromotionsBackend): PromotionsBackend

    companion object {
        @Provides
        @Singleton
        fun provideTransactionRemote(mock: MockTransactionRepository): TransactionRemote = mock

        @Provides
        @Singleton
        fun provideConflictInbox(@ApplicationContext context: Context): ConflictInbox =
            ConflictInbox(File(context.filesDir, "sync-conflicts.txt"))
    }
}
