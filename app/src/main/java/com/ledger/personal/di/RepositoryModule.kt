package com.ledger.personal.di

import android.content.Context
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.remote.api.RetrofitHomeBoard
import com.ledger.personal.data.remote.api.RetrofitPaymentsBackend
import com.ledger.personal.data.remote.api.RetrofitPromotionsBackend
import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.data.repository.TransactionsRepository
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.data.security.BindingServer
import com.ledger.personal.data.security.DeviceBindingRetrofitApi
import com.ledger.personal.data.security.LocalBindingServer
import com.ledger.personal.data.security.RetrofitBindingServer
import retrofit2.Retrofit
import com.ledger.personal.domain.demo.DemoConditions
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.PaymentsBackend
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.demo.SteadyConditions
import com.ledger.personal.mock.MockHomeBoard
import com.ledger.personal.mock.MockPaymentsBackend
import com.ledger.personal.mock.MockPromotionsBackend
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.mock.SwitchConditions
import com.ledger.personal.feature.wallet.payment.PaymentRail
import java.io.File
import java.io.IOException
import dagger.Binds
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Hilt bindings for repository. */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    /** Supplies api config. */
    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        logRequests = BuildConfig.ENABLE_HTTP_LOGGING,
        debuggable = BuildConfig.DEBUG,
        pinSha256 = BuildConfig.PIN_SHA256,
        pinBackup = BuildConfig.PIN_SHA256_BACKUP,
        paymentsBaseUrl = BuildConfig.PAYMENTS_BASE_URL,
        promotionsBaseUrl = BuildConfig.PROMOTIONS_BASE_URL,
        mockFlavor = BuildConfig.FLAVOR == "mock",
    )

    /** Supplies transaction remote. */
    @Provides
    @Singleton
    fun provideTransactionRemote(
        mockRepository: MockTransactionRepository,
        transactionsRepository: TransactionsRepository,
    ): TransactionRemote = if (BuildConfig.FLAVOR == "mock") mockRepository else transactionsRepository

    /** Supplies conflict inbox. */
    @Provides
    @Singleton
    fun provideConflictInbox(@ApplicationContext context: Context): ConflictInbox =
        ConflictInbox(File(context.filesDir, "sync-conflicts.txt"))

    /** Supplies binding server Retrofit API. */
    @Provides
    @Singleton
    fun provideDeviceBindingRetrofitApi(
        @AuthenticatedClient retrofit: Retrofit,
    ): DeviceBindingRetrofitApi = retrofit.create(DeviceBindingRetrofitApi::class.java)

    /** Supplies binding server. */
    @Provides
    @Singleton
    fun provideBindingServer(local: LocalBindingServer, remote: RetrofitBindingServer): BindingServer =
        if (BuildConfig.FLAVOR == "mock") local else remote
}

/** Picks the mock or live backend for each team. Their response shapes stay inside the Retrofit classes. */
@Module
@InstallIn(SingletonComponent::class)
object TeamModule {
    /** Supplies home board. */
    @Provides
    @Singleton
    fun home(mock: MockHomeBoard, live: RetrofitHomeBoard): HomeBoard = if (BuildConfig.FLAVOR == "mock") mock else live

    /** Supplies payments backend. */
    @Provides
    @Singleton
    fun payments(mock: MockPaymentsBackend, live: RetrofitPaymentsBackend): PaymentsBackend =
        if (BuildConfig.FLAVOR == "mock") mock else live

    /** Supplies promotions backend. */
    @Provides
    @Singleton
    fun promotions(mock: MockPromotionsBackend, live: RetrofitPromotionsBackend): PromotionsBackend =
        if (BuildConfig.FLAVOR == "mock") mock else live
}

/** Picks demo levers for this flavor. Live builds keep them off. */
@Module
@InstallIn(SingletonComponent::class)
object DemoModule {
    /** Supplies demo conditions. */
    @Provides
    @Singleton
    fun conditions(mock: SwitchConditions): DemoConditions =
        if (BuildConfig.FLAVOR == "mock") mock else SteadyConditions

    /** Supplies the payment attempt. Only the mock flavor runs the flaky rail. */
    @Provides
    fun paymentRail(): PaymentRail = if (BuildConfig.FLAVOR == "mock") {
        PaymentRail { post -> MockSwitches.payment(post = post) }
    } else {
        PaymentRail { post -> post() }
    }
}
