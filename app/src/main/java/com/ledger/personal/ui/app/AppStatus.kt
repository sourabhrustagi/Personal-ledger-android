package com.ledger.personal.ui.app

import com.ledger.personal.data.prefs.AppPrefs

/** Which shell LedgerOneApp should show. The view model updates this from prefs, release, and session. */
sealed class AppStatus {
    /** Prefs or the version check are still loading. */
    data object Loading : AppStatus()

    /** This install is below the published versionCode. */
    data object Update : AppStatus()

    /** First-run onboarding. */
    data object Onboard : AppStatus()

    /** MPIN or biometric, then a server session. */
    data class Locked(val prefs: AppPrefs, val session: SessionState) : AppStatus()

    /** Signed-in personal tabs. */
    data class Personal(val prefs: AppPrefs) : AppStatus()

    /** Signed-in POS or fraud console. */
    data class Work(val mode: AppMode) : AppStatus()
}
