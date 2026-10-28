package com.ledger.personal.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.BuildConfig
import com.ledger.personal.data.prefs.AppPreferences
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.data.prefs.CategorySync
import com.ledger.personal.domain.release.AppRelease
import com.ledger.personal.domain.release.softUpdateDue
import com.ledger.personal.domain.release.updateRequired
import com.ledger.personal.domain.session.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.ledger.personal.mock.MockValues
import com.ledger.personal.ui.components.HubFlags
import javax.inject.Inject

/** Session state. */
data class SessionState(
    val unlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedUntilMs: Long = 0L,
    val revealBalances: Boolean = false,
    val signInMessage: String? = null,
)

/** Alias for backward compatibility. */
typealias OtpUiState = com.ledger.personal.feature.auth.OtpUiState

/** True when the mock switch is on or the phone has no internet. */
fun offlineNow(mockOffline: Boolean, connected: Boolean): Boolean = mockOffline || !connected

const val MOCK_MPIN = MockValues.MPIN
fun mockPinUnlocks(pin: String, mockFlavor: Boolean): Boolean = mockFlavor && pin == MOCK_MPIN

/**
 * Application-level shell lifecycle coordinator.
 * Adheres to SRP by managing solely top-level app status evaluation,
 * session lock/unlock state, and global category availability.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val store: AppPreferences,
    categorySync: CategorySync,
    release: AppRelease,
    private val sessionManager: Session,
) : ViewModel() {

    val prefs: StateFlow<AppPrefs?> = store.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableCategoriesReady = MutableStateFlow(false)

    /** False until GET static/categories has returned. */
    val categoriesReady: StateFlow<Boolean> = mutableCategoriesReady.asStateFlow()

    private val mutableReleaseChecked = MutableStateFlow(false)
    private val mutableUpdateRequired = MutableStateFlow(false)
    private val mutableSoftUpdate = MutableStateFlow(false)
    private var offeredVersionCode = 0

    private val mutableSession = MutableStateFlow(SessionState())
    val session: StateFlow<SessionState> = mutableSession.asStateFlow()

    /** Splash, update, onboard, lock, or the signed-in shell. */
    val status: StateFlow<AppStatus> = combine(
        prefs,
        session,
        mutableReleaseChecked,
        mutableUpdateRequired,
        mutableSoftUpdate,
    ) { prefs, session, ready, blocked, softUpdate ->
        appStatus(prefs, session, ready, blocked, softUpdate)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppStatus.Loading)

    init {
        viewModelScope.launch {
            categorySync.pull()
            mutableCategoriesReady.value = true
        }
        viewModelScope.launch {
            val notice = release.notice()
            val ignored = store.prefs.first().ignoredUpdateCode
            offeredVersionCode = notice.softVersionCode
            mutableUpdateRequired.value = updateRequired(BuildConfig.VERSION_CODE, notice.minimumVersionCode)
            mutableSoftUpdate.value = softUpdateDue(notice.softUpdate, notice.softVersionCode, ignored)
            mutableReleaseChecked.value = true
        }
    }

    /** Hides the soft-update prompt until the next launch. */
    fun dismissUpdateOffer() {
        mutableSoftUpdate.value = false
    }

    /** Remembers the versionCode from GET static/status so that offer stays hidden. */
    fun ignoreUpdate() {
        val code = offeredVersionCode
        mutableSoftUpdate.value = false
        if (code > 0) viewModelScope.launch { store.ignoreUpdate(code) }
    }

    /** Unlocks the active session. */
    fun unlock() {
        viewModelScope.launch {
            sessionManager.begin().fold(
                onSuccess = {
                    mutableSession.update { it.copy(unlocked = true, failedAttempts = 0, signInMessage = null) }
                },
                onFailure = {
                    mutableSession.update { it.copy(unlocked = false, signInMessage = "Couldn't sign in. Try again.") }
                },
            )
        }
    }

    /** Drops the bearer token and locks the UI. */
    fun lock() {
        sessionManager.end()
        mutableSession.update {
            it.copy(unlocked = false, revealBalances = false, signInMessage = null)
        }
    }

    /** Resets the local session state (e.g. after full sign-out). */
    fun resetSession() {
        mutableSession.value = SessionState()
    }

    /** Toggles masked balance visibility. */
    fun toggleReveal() = mutableSession.update { it.copy(revealBalances = !it.revealBalances) }
}
