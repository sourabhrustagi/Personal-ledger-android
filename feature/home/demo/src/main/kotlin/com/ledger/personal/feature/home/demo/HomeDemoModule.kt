package com.ledger.personal.feature.home.demo

import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.SyncPhase
import com.ledger.personal.mock.MockHomeBoard
import com.ledger.personal.mock.MockPromotionsBackend
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HomeDemoModule {

    @Binds
    @Singleton
    abstract fun bindHomeBoard(mock: MockHomeBoard): HomeBoard

    @Binds
    @Singleton
    abstract fun bindPromotionsBackend(mock: MockPromotionsBackend): PromotionsBackend

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(mock: MockTransactionRepository): TransactionRepository

    companion object {
        @Provides
        @Singleton
        fun provideSyncCoordinator(): SyncCoordinator = object : SyncCoordinator {
            override fun enqueue() = Unit
            override fun observe(): Flow<SyncPhase> = flowOf(SyncPhase.SUCCEEDED)
        }
    }
}
