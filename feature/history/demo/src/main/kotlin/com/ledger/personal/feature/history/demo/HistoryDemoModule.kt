package com.ledger.personal.feature.history.demo

import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HistoryDemoModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(mock: MockTransactionRepository): TransactionRepository
}
