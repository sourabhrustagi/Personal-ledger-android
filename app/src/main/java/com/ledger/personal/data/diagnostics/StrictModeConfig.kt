package com.ledger.personal.data.diagnostics

import android.os.Build
import android.os.StrictMode
import timber.log.Timber

/**
 * Diagnostic StrictMode configurations to catch main thread disk I/O,
 * network calls, unclosed cursors, and memory leaks during debug cycles.
 */
object StrictModeConfig {

    /**
     * Installs StrictMode thread and VM policies in debug environments.
     */
    fun install() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .detectDiskWrites()
            .detectNetwork()
            .penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())

        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectLeakedSqlLiteObjects()
            .detectLeakedClosableObjects()
            .penaltyLog()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            vmPolicyBuilder.detectNonSdkApiUsage()
        }

        StrictMode.setVmPolicy(vmPolicyBuilder.build())
        Timber.d("StrictMode thread and VM policies installed successfully")
    }
}
