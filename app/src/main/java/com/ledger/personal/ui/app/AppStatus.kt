package com.ledger.personal.ui.app

import com.ledger.personal.data.prefs.AppPrefs

/** Which shell LedgerOneApp should show. The view model updates this from prefs, release, and session. */
sealed class AppStatus {
    /** True when GET static/status offers an update this shell can skip. */
    open val softUpdate: Boolean = false

    /** Prefs or the version check are still loading. */
    data object Loading : AppStatus()

    /** This install is below the published versionCode. */
    data object Update : AppStatus()

    /** First-run onboarding. */
    data class Onboard(override val softUpdate: Boolean = false) : AppStatus()

    /** MPIN or biometric, then a server session. */
    data class Locked(
        val prefs: AppPrefs,
        val session: SessionState,
        override val softUpdate: Boolean = false,
    ) : AppStatus()

    /** Signed-in personal tabs. */
    data class Personal(val prefs: AppPrefs, override val softUpdate: Boolean = false) : AppStatus()
}

/** Picks the shell. Force update wins as soon as the version check says this install is retired. */
fun appStatus(
    prefs: AppPrefs?,
    session: SessionState,
    releaseChecked: Boolean,
    updateRequired: Boolean,
    softUpdate: Boolean = false,
): AppStatus = when {
    prefs == null || !releaseChecked -> AppStatus.Loading
    updateRequired -> AppStatus.Update
    !prefs.onboarded -> AppStatus.Onboard(softUpdate)
    !session.unlocked -> AppStatus.Locked(prefs, session, softUpdate)
    else -> AppStatus.Personal(prefs, softUpdate)
}
