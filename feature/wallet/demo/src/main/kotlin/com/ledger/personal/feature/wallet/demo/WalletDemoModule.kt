package com.ledger.personal.feature.wallet.demo

import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.ui.feature.PaymentRail
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WalletDemoModule {

    @Provides
    @Singleton
    fun providePaymentRail(): PaymentRail = PaymentRail { post ->
        MockSwitches.payment(post = post)
    }
}
