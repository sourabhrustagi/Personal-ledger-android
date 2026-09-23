package com.ledger.personal.di

import com.ledger.personal.data.repository.FakeTransactionRepository
import com.ledger.personal.data.repository.RetrofitTransactionRepository
import com.ledger.personal.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds the repository implementation.
     * Starts with FakeTransactionRepository for standalone UI development and testability,
     * and can be swapped to RetrofitTransactionRepository when the remote API endpoint is connected.
     */
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(
        fakeRepository: FakeTransactionRepository
    ): TransactionRepository
}

@Module
@InstallIn(SingletonComponent::class)
object FakeRepositoryModule {
    @Provides
    @Singleton
    fun provideFakeTransactionRepository(): FakeTransactionRepository {
        return FakeTransactionRepository()
    }
}
