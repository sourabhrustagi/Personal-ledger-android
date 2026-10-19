package com.ledger.personal.ui.app

import android.content.Context
import com.ledger.personal.BuildConfig
import com.ledger.personal.mock.MockValues
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.data.prefs.AppPreferences
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.data.prefs.CategorySync
import com.ledger.personal.data.security.BindingServer
import com.ledger.personal.data.security.DeviceKeys
import com.ledger.personal.data.security.PIN_V2
import com.ledger.personal.domain.release.AppRelease
import com.ledger.personal.domain.release.softUpdateDue
import com.ledger.personal.domain.release.updateRequired
import com.ledger.personal.domain.session.Session
import com.ledger.personal.ui.app.auth.promptWithCrypto
import com.ledger.personal.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/** Hub flags. */
data class HubFlags(val offline: Boolean, val empty: Boolean, val reveal: Boolean, val onToggleReveal: () -> Unit)

/** Session state. */
data class SessionState(
    val unlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedUntilMs: Long = 0L,
    val revealBalances: Boolean = false,
    val signInMessage: String? = null,
)

const val MAX_MPIN_ATTEMPTS = 3
const val LOCKOUT_MS = 30_000L
const val MOCK_MPIN = MockValues.MPIN

/** Mock builds accept this PIN on top of the one set during onboarding. Live builds never do. */
fun mockPinUnlocks(pin: String, mockFlavor: Boolean): Boolean = mockFlavor && pin == MOCK_MPIN

/** True when the mock switch is on or the phone has no internet. */
fun offlineNow(mockOffline: Boolean, connected: Boolean): Boolean = mockOffline || !connected

/** State and actions for the app screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val store: AppPreferences,
    private val keys: DeviceKeys,
    private val server: BindingServer,
    categorySync: CategorySync,
    release: AppRelease,
    private val signIn: Session,
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

    /** Finishes onboarding. */
    fun completeOnboarding(name: String, phone: String, pin: String, biometric: Boolean, language: String) {
        viewModelScope.launch {
            store.completeOnboarding(name, phone, pin, biometric, language)
            openSession()
        }
    }

    /** Checks the PIN off the main thread. [onResult] runs on the main thread after a server session is opened. */
    fun tryMpin(pin: String, now: Long = System.currentTimeMillis(), onResult: (Boolean) -> Unit) {
        val current = prefs.value ?: return onResult(false)
        val demo = mockPinUnlocks(pin, BuildConfig.FLAVOR == "mock")
        if (!demo && now < mutableSession.value.lockedUntilMs) return onResult(false)
        viewModelScope.launch {
            val ok = demo || withContext(Dispatchers.Default) { store.matches(pin, current) }
            if (!ok) {
                mutableSession.update {
                    when {
                        it.failedAttempts + 1 >= MAX_MPIN_ATTEMPTS ->
                            it.copy(failedAttempts = 0, lockedUntilMs = now + LOCKOUT_MS, signInMessage = null)
                        else -> it.copy(failedAttempts = it.failedAttempts + 1, signInMessage = null)
                    }
                }
                onResult(false)
                return@launch
            }
            if (!demo && !current.mpinHash.startsWith(PIN_V2)) changeMpin(pin)
            onResult(openSession())
        }
    }

    /** Unlocks with biometric, then opens a server session. */
    fun unlockWithBiometric(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch { onResult(openSession()) }
    }

    /** Drops the bearer token and locks the UI. */
    fun lock() {
        signIn.end()
        mutableSession.update {
            it.copy(unlocked = false, revealBalances = false, signInMessage = null)
        }
    }

    /** Posts a new session. Stays locked when the server refuses. */
    private suspend fun openSession(): Boolean = signIn.begin().fold(
        onSuccess = {
            mutableSession.update { it.copy(unlocked = true, failedAttempts = 0, signInMessage = null) }
            true
        },
        onFailure = {
            mutableSession.update { it.copy(unlocked = false, signInMessage = "Couldn't sign in. Try again.") }
            false
        },
    )

    /** Toggles reveal. */
    fun toggleReveal() = mutableSession.update { it.copy(revealBalances = !it.revealBalances) }

    /** Stores theme. */
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { store.setTheme(mode) }

    /** Stores biometric. */
    fun setBiometric(enabled: Boolean) = viewModelScope.launch { store.setBiometric(enabled) }

    /** Stores hide balances. */
    fun setHideBalances(hide: Boolean) = viewModelScope.launch { store.setHideBalances(hide) }

    /** Changes mpin. */
    fun changeMpin(pin: String) = viewModelScope.launch { store.changeMpin(pin) }

    /** Stores categories. */
    fun setCategories(names: List<String>) = viewModelScope.launch { store.setCategories(names) }

    /** Binds this phone: Keystore key, biometric-unlocked signature over a server challenge, then registration. */
    fun bindDevice(context: Context, onDone: (Result<String>) -> Unit) {
        viewModelScope.launch {
            val challenge = runCatching { server.challenge() }
                .onFailure { if (it is CancellationException) throw it }
                .getOrElse { return@launch onDone(Result.failure(it)) }
            val publicKey = runCatching { keys.createKey() }.getOrElse { return@launch onDone(Result.failure(it)) }
            val unsigned = runCatching { keys.signer() }.getOrElse { return@launch onDone(Result.failure(it)) }
            promptWithCrypto(context, "Bind this phone", unsigned) { signer ->
                if (signer == null) return@promptWithCrypto onDone(Result.failure(IllegalStateException("Biometric check cancelled")))
                viewModelScope.launch {
                    val signature = signer.run {
                        update(challenge)
                        sign()
                    }
                    val result = server.register(publicKey, challenge, signature)
                    result.onSuccess {
                        store.setDeviceId(it)
                        store.setBiometric(true)
                    }
                    onDone(result)
                }
            }
        }
    }

    /** Ends this sign-in. The ledger stays on the phone and opens again with the MPIN. */
    fun logOut() = lock()

    /** Sign out everywhere. */
    fun signOutEverywhere() = viewModelScope.launch {
        signIn.end()
        keys.delete()
        store.reset()
        mutableSession.value = SessionState()
    }
}
