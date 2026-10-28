package com.ledger.personal.data.sync

import android.content.Context
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
import com.ledger.personal.domain.settings.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionSyncWorkerTest {

    @Test
    fun workerRunsOnAnyConnection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        var recordedSyncTime: Long? = null
        val fakeSettings = object : UserSettings {
            override val lastSyncEpochMs: Flow<Long?> = flowOf(null)
            override suspend fun recordSync(epochMs: Long) {
                recordedSyncTime = epochMs
            }
        }
        val config = androidx.work.Configuration.Builder()
            .setWorkerFactory(object : WorkerFactory() {
                override fun createWorker(
                    appContext: Context,
                    workerClassName: String,
                    workerParameters: WorkerParameters,
                ): ListenableWorker = TransactionSyncWorker(
                    appContext,
                    workerParameters,
                    fakeSettings,
                )
            })
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        val constraints = TransactionSync.constraints
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertFalse(constraints.requiresBatteryNotLow())

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            TransactionSync.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            TransactionSync.request(),
        ).result.get()
        val queued = workManager.getWorkInfosForUniqueWork(TransactionSync.WORK_NAME).get().single()
        assertEquals(WorkInfo.State.ENQUEUED, queued.state)

        runBlocking {
            val workerFactory = config.workerFactory
            val worker = androidx.work.testing.TestListenableWorkerBuilder<TransactionSyncWorker>(context)
                .setWorkerFactory(workerFactory)
                .build()
            val result = worker.doWork()
            assertTrue(result is ListenableWorker.Result.Success)
            assertTrue(recordedSyncTime != null)
        }
    }
}

