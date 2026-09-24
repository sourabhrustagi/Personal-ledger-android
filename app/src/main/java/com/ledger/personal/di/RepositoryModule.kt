package com.ledger.personal.di

import com.ledger.personal.data.repository.RetrofitTransactionRepository
import com.ledger.personal.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        retrofitRepository: RetrofitTransactionRepository
    ): TransactionRepository
}
