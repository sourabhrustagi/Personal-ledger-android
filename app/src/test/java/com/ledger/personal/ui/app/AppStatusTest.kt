package com.ledger.personal.ui.app

import com.ledger.personal.data.prefs.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStatusTest {
    @Test
    fun aRetiredBuildBlocksTheShell() {
        val onboarded = AppPrefs(onboarded = true, name = "Asha")
        assertEquals(AppStatus.Update, appStatus(onboarded, SessionState(unlocked = true), releaseChecked = true, updateRequired = true))
        assertEquals(AppStatus.Update, appStatus(AppPrefs(), SessionState(), releaseChecked = true, updateRequired = true))
    }

    @Test
    fun forceUpdateDoesNotApplyUntilTheVersionCheckFinishes() {
        assertEquals(AppStatus.Loading, appStatus(AppPrefs(), SessionState(), releaseChecked = false, updateRequired = true))
        assertEquals(AppStatus.Loading, appStatus(null, SessionState(), releaseChecked = true, updateRequired = true))
    }

    @Test
    fun anAllowedBuildContinues() {
        val prefs = AppPrefs(onboarded = true, name = "Asha")
        assertTrue(appStatus(prefs, SessionState(unlocked = true), releaseChecked = true, updateRequired = false) is AppStatus.Personal)
        assertTrue(appStatus(AppPrefs(), SessionState(), releaseChecked = true, updateRequired = false) is AppStatus.Onboard)
    }

    @Test
    fun aSoftUpdateStaysOnTheShell() {
        val prefs = AppPrefs(onboarded = true, name = "Asha")
        val personal = appStatus(prefs, SessionState(unlocked = true), releaseChecked = true, updateRequired = false, softUpdate = true)
        assertTrue(personal is AppStatus.Personal && personal.softUpdate)
        val blocked = appStatus(prefs, SessionState(unlocked = true), releaseChecked = true, updateRequired = true, softUpdate = true)
        assertEquals(AppStatus.Update, blocked)
    }
}
