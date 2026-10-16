package com.ledger.personal.di

import android.content.Context
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.remote.api.KtorAssistantBackend
import com.ledger.personal.data.remote.api.KtorHomeBoard
import com.ledger.personal.data.remote.api.KtorInsightsBackend
import com.ledger.personal.data.remote.api.KtorPaymentsBackend
import com.ledger.personal.data.remote.api.KtorPromotionsBackend
import com.ledger.personal.data.local.MockTickerSocket
import com.ledger.personal.data.local.KtorTickerSocket
import com.ledger.personal.data.local.TickerSocket
import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.data.repository.KtorTransactionRepository
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.data.security.BindingServer
import com.ledger.personal.data.security.KtorBindingServer
import com.ledger.personal.data.security.LocalBindingServer
import com.ledger.personal.domain.demo.DemoConditions
import com.ledger.personal.domain.board.AssistantBackend
import com.ledger.personal.domain.board.HomeBoard
import com.ledger.personal.domain.board.InsightsBackend
import com.ledger.personal.domain.board.PaymentsBackend
import com.ledger.personal.domain.board.PromotionsBackend
import com.ledger.personal.domain.demo.SteadyConditions
import com.ledger.personal.mock.MockAssistantBackend
import com.ledger.personal.mock.MockHomeBoard
import com.ledger.personal.mock.MockInsightsBackend
import com.ledger.personal.mock.MockPaymentsBackend
import com.ledger.personal.mock.MockPromotionsBackend
import com.ledger.personal.mock.MockSwitches
import com.ledger.personal.mock.SwitchConditions
import com.ledger.personal.ui.feature.PaymentRail
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.onEach
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
        insightsBaseUrl = BuildConfig.INSIGHTS_BASE_URL,
        assistantBaseUrl = BuildConfig.ASSISTANT_BASE_URL,
        promotionsBaseUrl = BuildConfig.PROMOTIONS_BASE_URL,
        localSession = BuildConfig.FLAVOR == "mock",
    )

    /** Supplies transaction remote. */
    @Provides
    @Singleton
    fun provideTransactionRemote(
        mockRepository: MockTransactionRepository,
        ktorRepository: KtorTransactionRepository,
    ): TransactionRemote = if (BuildConfig.FLAVOR == "mock") mockRepository else ktorRepository

    /** Supplies ticker. */
    @Provides
    @Singleton
    fun provideTicker(@AuthenticatedClient client: HttpClient): TickerSocket = if (BuildConfig.FLAVOR == "mock") {
        val mock = MockTickerSocket()
        object : TickerSocket {
            /** Prices. */
            override fun prices() = mock.prices().onEach {
                if (MockSwitches.dropLive.value) throw IOException("Live connection dropped (mock)")
            }
        }
    } else {
        KtorTickerSocket(client, BuildConfig.TICKER_URL)
    }

    /** Supplies conflict inbox. */
    @Provides
    @Singleton
    fun provideConflictInbox(@ApplicationContext context: Context): ConflictInbox =
        ConflictInbox(File(context.filesDir, "sync-conflicts.txt"))

    /** Supplies binding server. */
    @Provides
    @Singleton
    fun provideBindingServer(local: LocalBindingServer, ktor: KtorBindingServer): BindingServer =
        if (BuildConfig.FLAVOR == "mock") local else ktor
}

/** Picks the mock or live backend for each team. Their response shapes stay inside the Ktor classes. */
@Module
@InstallIn(SingletonComponent::class)
object TeamModule {
    /** Supplies home board. */
    @Provides
    @Singleton
    fun home(mock: MockHomeBoard, live: KtorHomeBoard): HomeBoard = if (BuildConfig.FLAVOR == "mock") mock else live

    /** Supplies payments backend. */
    @Provides
    @Singleton
    fun payments(mock: MockPaymentsBackend, live: KtorPaymentsBackend): PaymentsBackend =
        if (BuildConfig.FLAVOR == "mock") mock else live

    /** Supplies insights backend. */
    @Provides
    @Singleton
    fun insights(mock: MockInsightsBackend, live: KtorInsightsBackend): InsightsBackend =
        if (BuildConfig.FLAVOR == "mock") mock else live

    /** Supplies assistant backend. */
    @Provides
    @Singleton
    fun assistant(mock: MockAssistantBackend, live: KtorAssistantBackend): AssistantBackend =
        if (BuildConfig.FLAVOR == "mock") mock else live

    /** Supplies promotions backend. */
    @Provides
    @Singleton
    fun promotions(mock: MockPromotionsBackend, live: KtorPromotionsBackend): PromotionsBackend =
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
        if (BuildConfig.FLAVOR == "mock") mock else SteadyConditions()

    /** Supplies the payment attempt. Only the mock flavor runs the flaky rail. */
    @Provides
    fun paymentRail(): PaymentRail = if (BuildConfig.FLAVOR == "mock") {
        PaymentRail { post -> MockSwitches.payment(post = post) }
    } else {
        PaymentRail { post -> post() }
    }
}

/** Hilt bindings for offline. */
@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineModule {
    /** Binds kyc drafts. */
    @Binds
    @Singleton
    abstract fun bindKycDrafts(store: com.ledger.personal.data.local.RoomKycDraftStore): com.ledger.personal.data.local.KycDraftStore

    /** Binds assistant history. */
    @Binds
    @Singleton
    abstract fun bindAssistantHistory(
        store: com.ledger.personal.data.local.RoomAssistantHistory,
    ): com.ledger.personal.data.local.AssistantHistory
}
