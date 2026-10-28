package com.ledger.personal.feature.auth.demo

import com.ledger.personal.di.ApiConfig
import com.ledger.personal.domain.session.OtpRepository
import com.ledger.personal.domain.session.OtpResponse
import com.ledger.personal.mock.MockValues
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthDemoModule {

    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(
        baseUrl = "http://10.0.2.2:8080/retail/v1/",
        logRequests = true,
        debuggable = true,
        pinSha256 = "",
        pinBackup = "",
        paymentsBaseUrl = "http://10.0.2.2:8091/v1/",
        promotionsBaseUrl = "http://10.0.2.2:8094/v1/",
        mockFlavor = true,
    )
}
