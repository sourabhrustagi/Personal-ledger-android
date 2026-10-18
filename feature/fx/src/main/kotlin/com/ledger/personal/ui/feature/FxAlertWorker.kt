package com.ledger.personal.ui.feature

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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

/** Allowed values of fx notify. */
enum class FxNotify { POST, SKIP, QUIET }

/** Fx notify decision. */
fun fxNotifyDecision(permissionGranted: Boolean, breached: Boolean): FxNotify = when {
    !permissionGranted -> FxNotify.SKIP
    breached -> FxNotify.POST
    else -> FxNotify.QUIET
}

/** Shared fx alert schedule. */
object FxAlertSchedule {
    const val PERIODIC = "ledger-fx-alert"
    const val NOW = "ledger-fx-alert-now"

    /** Constraints. */
    private fun constraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /** Queues periodic. */
    fun enqueuePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<FxAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /** Queues now. */
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

/** Background work for fx alert. */
@HiltWorker
class FxAlertWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
) : CoroutineWorker(context, params) {
    /** Do work. */
    override suspend fun doWork(): Result {
        val runtimeGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val granted = runtimeGranted && NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()
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

    /** Notify breach. */
    private fun notifyBreach() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
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
        try {
            manager.notify(1, notification)
        } catch (_: SecurityException) {
            return
        }
    }

    companion object {
        const val KEY_BREACH = "breach"
        const val KEY_PERMISSION = "permission"
        private const val CHANNEL = "fx-alerts"
    }
}
