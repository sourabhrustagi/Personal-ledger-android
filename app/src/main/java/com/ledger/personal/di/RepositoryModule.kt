package com.ledger.personal.di

import android.content.Context
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.local.CardReader
import com.ledger.personal.data.local.FakeCardReader
import com.ledger.personal.data.local.FakeReceiptPrinter
import com.ledger.personal.data.local.FakeTickerSocket
import com.ledger.personal.data.local.OkHttpTickerSocket
import com.ledger.personal.data.local.ReceiptPrinter
import com.ledger.personal.data.local.TickerSocket
import com.ledger.personal.data.repository.FakeTransactionRepository
import com.ledger.personal.data.repository.RetrofitTransactionRepository
import com.ledger.personal.domain.repository.TransactionRemote
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.data.security.BindingServer
import com.ledger.personal.data.security.DeviceApi
import com.ledger.personal.data.security.LocalBindingServer
import com.ledger.personal.data.security.RetrofitBindingServer
import com.ledger.personal.mock.MockSwitches
import retrofit2.Retrofit
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

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideApiConfig(): ApiConfig = ApiConfig(
        baseUrl = BuildConfig.API_BASE_URL,
        logBodies = BuildConfig.ENABLE_HTTP_LOGGING,
        debuggable = BuildConfig.DEBUG,
        pinSha256 = BuildConfig.PIN_SHA256,
        pinBackup = BuildConfig.PIN_SHA256_BACKUP,
    )

    @Provides
    @Singleton
    fun provideTransactionRemote(
        fakeRepository: FakeTransactionRepository,
        retrofitRepository: RetrofitTransactionRepository,
    ): TransactionRemote = if (BuildConfig.USE_FAKE_REPOSITORY) fakeRepository else retrofitRepository

    @Provides
    @Singleton
    fun provideTicker(client: okhttp3.OkHttpClient): TickerSocket = if (BuildConfig.USE_FAKE_REPOSITORY) {
        val fake = FakeTickerSocket()
        object : TickerSocket {
            override fun prices() = fake.prices().onEach {
                if (MockSwitches.dropLive.value) throw IOException("Live connection dropped (mock)")
            }
        }
    } else {
        OkHttpTickerSocket(client, BuildConfig.TICKER_URL)
    }

    @Provides
    @Singleton
    fun provideConflictInbox(@ApplicationContext context: Context): ConflictInbox =
        ConflictInbox(File(context.filesDir, "sync-conflicts.txt"))

    @Provides
    @Singleton
    fun provideBindingServer(local: LocalBindingServer, retrofit: Retrofit): BindingServer =
        if (BuildConfig.USE_FAKE_REPOSITORY) local else RetrofitBindingServer(retrofit.create(DeviceApi::class.java))

    @Provides
    @Singleton
    fun provideCardReader(): CardReader = FakeCardReader()

    @Provides
    @Singleton
    fun provideReceiptPrinter(): ReceiptPrinter = FakeReceiptPrinter()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class OfflineModule {
    @Binds
    @Singleton
    abstract fun bindKycDrafts(store: com.ledger.personal.data.local.RoomKycDraftStore): com.ledger.personal.data.local.KycDraftStore

    @Binds
    @Singleton
    abstract fun bindAssistantHistory(
        store: com.ledger.personal.data.local.RoomAssistantHistory,
    ): com.ledger.personal.data.local.AssistantHistory

    @Binds
    @Singleton
    abstract fun bindPosForward(store: com.ledger.personal.data.local.RoomPosForward): com.ledger.personal.data.local.PosForwardStore
}
