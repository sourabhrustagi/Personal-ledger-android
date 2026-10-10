package com.ledger.personal.data.sync

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import java.util.concurrent.TimeUnit

/** Shared transaction sync. */
object TransactionSync {
    const val WORK_NAME = "ledger-transaction-sync"

    /** Any connection is enough. A low battery must not hold a write that is already queued. */
    fun constraints(): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Request. */
    fun request(): OneTimeWorkRequest = OneTimeWorkRequestBuilder<TransactionSyncWorker>()
        .setConstraints(constraints())
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
        .build()
}
