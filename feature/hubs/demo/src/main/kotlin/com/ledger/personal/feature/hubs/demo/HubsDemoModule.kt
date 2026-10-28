package com.ledger.personal.feature.hubs.demo

import com.ledger.personal.domain.board.PromoTab
import com.ledger.personal.domain.board.PromotionRepository
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.mock.MockPromotionsBackend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object HubsDemoModule {

    @Provides
    @Singleton
    fun providePromotionsBackend(mock: MockPromotionsBackend): PromotionsBackend = mock

    @Provides
    @Singleton
    fun providePromotionRepository(backend: MockPromotionsBackend): PromotionRepository = object : PromotionRepository {
        override fun observePromotions(): Flow<List<PromoTab>> = flow {
            emit(backend.tabs().getOrDefault(emptyList()))
        }
    }
}
