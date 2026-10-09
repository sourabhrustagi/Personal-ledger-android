package com.ledger.personal.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.WorkManagerTestInitHelper
import com.ledger.personal.data.local.LedgerDatabase
import com.ledger.personal.data.repository.FakeTransactionRepository
import com.ledger.personal.data.repository.OfflineTransactionRepository
import com.ledger.personal.domain.settings.UserSettings
import com.ledger.personal.domain.sync.ConflictInbox
import com.ledger.personal.domain.sync.SyncCoordinator
import com.ledger.personal.domain.sync.SyncPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionSyncWorkerTest {

    @Test
    fun workerRunsWhenNetworkAndBatteryConstraintsAreMet() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, LedgerDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val repository = OfflineTransactionRepository(
            dao = database.transactionDao(),
            remote = FakeTransactionRepository(),
            conflicts = ConflictInbox(null),
            syncCoordinator = object : SyncCoordinator {
                override fun enqueue() = Unit
                override fun observe(): Flow<SyncPhase> = flowOf(SyncPhase.IDLE)
            },
        )
        val config = androidx.work.Configuration.Builder()
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = TransactionSyncWorker(
                    appContext,
                    workerParameters,
                    repository,
                    object : UserSettings {
                        override val lastSyncEpochMs: Flow<Long?> = flowOf(null)
                        override suspend fun recordSync(epochMs: Long) = Unit
                    },
                )
            })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        val constraints = TransactionSync.constraints()
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertTrue(constraints.requiresBatteryNotLow())

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            TransactionSync.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            TransactionSync.request(),
        ).result.get()
        val queued = workManager.getWorkInfosForUniqueWork(TransactionSync.WORK_NAME).get().single()
        assertEquals(WorkInfo.State.ENQUEUED, queued.state)

        runBlocking {
            assertTrue(repository.sync().isSuccess)
            assertEquals(25, database.transactionDao().allVisible().size)
        }
        database.close()
    }
}
