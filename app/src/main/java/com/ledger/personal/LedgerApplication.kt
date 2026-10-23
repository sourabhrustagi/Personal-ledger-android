package com.ledger.personal

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.ledger.personal.data.diagnostics.StrictModeConfig
import com.ledger.personal.domain.sync.SyncCoordinator
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/** Ledger application. */
@HiltAndroidApp
class LedgerApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var syncCoordinator: SyncCoordinator

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /** Reacts to create. */
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            StrictModeConfig.install()
            Timber.d("Personal Ledger Application initialized in DEBUG mode with StrictMode")
        }
        syncCoordinator.enqueue()
    }
}
