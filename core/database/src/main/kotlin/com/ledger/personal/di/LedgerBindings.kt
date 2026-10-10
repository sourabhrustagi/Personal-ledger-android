package com.ledger.personal.di

import com.ledger.personal.data.repository.OfflineTransactionRepository
import com.ledger.personal.data.settings.DataStoreUserSettings
import com.ledger.personal.data.sync.WorkManagerSyncCoordinator
import com.ledger.personal.domain.repository.TransactionRepository
import com.ledger.personal.domain.settings.UserSettings
import com.ledger.personal.domain.sync.SyncCoordinator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Ledger bindings. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LedgerBindings {
    /** Binds transaction repository. */
    @Binds
    @Singleton
    abstract fun bindTransactionRepository(repository: OfflineTransactionRepository): TransactionRepository

    /** Binds sync coordinator. */
    @Binds
    @Singleton
    abstract fun bindSyncCoordinator(coordinator: WorkManagerSyncCoordinator): SyncCoordinator

    /** Binds user settings. */
    @Binds
    @Singleton
    abstract fun bindUserSettings(settings: DataStoreUserSettings): UserSettings
}
