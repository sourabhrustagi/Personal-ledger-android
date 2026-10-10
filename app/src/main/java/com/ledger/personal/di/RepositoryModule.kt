package com.ledger.personal.di

import android.content.Context
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.local.CardReader
import com.ledger.personal.data.local.MockCardReader
import com.ledger.personal.data.local.MockReceiptPrinter
import com.ledger.personal.data.local.MockTickerSocket
import com.ledger.personal.data.local.KtorTickerSocket
import com.ledger.personal.data.local.ReceiptPrinter
import com.ledger.personal.data.local.TickerSocket
import com.ledger.personal.data.repository.MockTransactionRepository
import com.ledger.personal.data.repository.KtorTransactionRepository
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.data.security.BindingServer
import com.ledger.personal.data.security.KtorBindingServer
import com.ledger.personal.data.security.LocalBindingServer
import com.ledger.personal.mock.MockSwitches
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

    /** Supplies card reader. */
    @Provides
    @Singleton
    fun provideCardReader(): CardReader = MockCardReader()

    /** Supplies receipt printer. */
    @Provides
    @Singleton
    fun provideReceiptPrinter(): ReceiptPrinter = MockReceiptPrinter()
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

    /** Binds pos forward. */
    @Binds
    @Singleton
    abstract fun bindPosForward(store: com.ledger.personal.data.local.RoomPosForward): com.ledger.personal.data.local.PosForwardStore
}
