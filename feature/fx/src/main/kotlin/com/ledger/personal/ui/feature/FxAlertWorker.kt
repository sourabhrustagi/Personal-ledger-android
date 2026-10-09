package com.ledger.personal.ui.feature

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

enum class FxNotify { POST, SKIP, QUIET }

fun fxNotifyDecision(permissionGranted: Boolean, breached: Boolean): FxNotify = when {
    !permissionGranted -> FxNotify.SKIP
    breached -> FxNotify.POST
    else -> FxNotify.QUIET
}

object FxAlertSchedule {
    const val PERIODIC = "ledger-fx-alert"
    const val NOW = "ledger-fx-alert-now"

    private fun constraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun enqueuePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<FxAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun enqueueNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<FxAlertWorker>()
            .setConstraints(constraints())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .setInputData(workDataOf(FxAlertWorker.KEY_BREACH to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(NOW, ExistingWorkPolicy.KEEP, request)
    }
}

@HiltWorker
class FxAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val granted = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
        val breached = inputData.getBoolean(KEY_BREACH, false)
        return when (fxNotifyDecision(granted, breached)) {
            FxNotify.SKIP -> Result.success(workDataOf(KEY_PERMISSION to "denied"))
            FxNotify.QUIET -> Result.success()
            FxNotify.POST -> {
                notifyBreach()
                Result.success()
            }
        }
    }

    private fun notifyBreach() {
        val manager = NotificationManagerCompat.from(applicationContext)
        val channel = NotificationChannel(CHANNEL, "FX alerts", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(channel)
        val intent = applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
            ?: return
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("FX threshold")
            .setContentText("A watched rate crossed its threshold")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(1, notification)
    }

    companion object {
        const val KEY_BREACH = "breach"
        const val KEY_PERMISSION = "permission"
        private const val CHANNEL = "fx-alerts"
    }
}
