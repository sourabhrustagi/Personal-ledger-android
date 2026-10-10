package com.ledger.personal.ui.app

import android.content.Context
import com.ledger.personal.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.data.prefs.AppPreferences
import com.ledger.personal.data.prefs.AppPrefs
import com.ledger.personal.data.prefs.CategorySync
import com.ledger.personal.data.security.BindingServer
import com.ledger.personal.data.security.DeviceKeys
import com.ledger.personal.data.security.PIN_V2
import com.ledger.personal.domain.release.AppRelease
import com.ledger.personal.domain.release.updateRequired
import com.ledger.personal.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Allowed values of app mode. */
enum class AppMode(val label: String) { PERSONAL("Personal"), POS("POS terminal"), FRAUD("Fraud ops") }

/** Hub flags. */
data class HubFlags(val offline: Boolean, val empty: Boolean, val reveal: Boolean, val onToggleReveal: () -> Unit)

/** Session state. */
data class SessionState(
    val unlocked: Boolean = false,
    val failedAttempts: Int = 0,
    val lockedUntilMs: Long = 0L,
    val mode: AppMode = AppMode.PERSONAL,
    val revealBalances: Boolean = false,
)

const val MAX_MPIN_ATTEMPTS = 3
const val LOCKOUT_MS = 30_000L
const val MOCK_MPIN = "1111"

/** Mock builds accept this PIN on top of the one set during onboarding. Live builds never do. */
fun mockPinUnlocks(pin: String, mockFlavor: Boolean): Boolean = mockFlavor && pin == MOCK_MPIN

/** Mock user holds every role so the switcher is demonstrable; a live build would read roles from the session token. */
val MOCK_ROLES = setOf("CUSTOMER", "MERCHANT", "ANALYST")

/** State and actions for the app screen. */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val store: AppPreferences,
    private val keys: DeviceKeys,
    private val server: BindingServer,
    categorySync: CategorySync,
    release: AppRelease,
) : ViewModel() {
    val prefs: StateFlow<AppPrefs?> = store.prefs.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val mutableCategoriesReady = MutableStateFlow(false)

    /** False until GET static/categories has returned. */
    val categoriesReady: StateFlow<Boolean> = mutableCategoriesReady.asStateFlow()

    private val mutableReleaseChecked = MutableStateFlow(false)

    /** False until GET static/version has returned. */
    val releaseChecked: StateFlow<Boolean> = mutableReleaseChecked.asStateFlow()

    private val mutableUpdateRequired = MutableStateFlow(false)

    /** True when BuildConfig.VERSION_CODE is below the static API versionCode. */
    val updateRequired: StateFlow<Boolean> = mutableUpdateRequired.asStateFlow()

    init {
        viewModelScope.launch {
            categorySync.pull()
            mutableCategoriesReady.value = true
        }
        viewModelScope.launch {
            mutableUpdateRequired.value = updateRequired(BuildConfig.VERSION_CODE, release.minimumVersionCode())
            mutableReleaseChecked.value = true
        }
    }

    private val mutableSession = MutableStateFlow(SessionState())
    val session: StateFlow<SessionState> = mutableSession.asStateFlow()

    /** Finishes onboarding. */
    fun completeOnboarding(name: String, phone: String, pin: String, biometric: Boolean, language: String) {
        viewModelScope.launch {
            store.completeOnboarding(name, phone, pin, biometric, language)
            mutableSession.update { it.copy(unlocked = true) }
        }
    }

    /** Checks the PIN off the main thread. [onResult] runs on the main thread. */
    fun tryMpin(pin: String, now: Long = System.currentTimeMillis(), onResult: (Boolean) -> Unit) {
        val current = prefs.value ?: return onResult(false)
        val demo = mockPinUnlocks(pin, BuildConfig.FLAVOR == "mock")
        if (!demo && now < mutableSession.value.lockedUntilMs) return onResult(false)
        viewModelScope.launch {
            val ok = demo || withContext(Dispatchers.Default) { store.matches(pin, current) }
            mutableSession.update {
                when {
                    ok -> it.copy(unlocked = true, failedAttempts = 0)
                    it.failedAttempts + 1 >= MAX_MPIN_ATTEMPTS -> it.copy(failedAttempts = 0, lockedUntilMs = now + LOCKOUT_MS)
                    else -> it.copy(failedAttempts = it.failedAttempts + 1)
                }
            }
            if (ok && !demo && !current.mpinHash.startsWith(PIN_V2)) changeMpin(pin)
            onResult(ok)
        }
    }

    /** Unlocks with biometric. */
    fun unlockWithBiometric() = mutableSession.update { it.copy(unlocked = true, failedAttempts = 0) }

    /** Lock. */
    fun lock() = mutableSession.update { it.copy(unlocked = false, revealBalances = false, mode = AppMode.PERSONAL) }

    /** Stores mode. */
    fun setMode(mode: AppMode) = mutableSession.update { it.copy(mode = mode) }

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
            val challenge = runCatching { server.challenge() }.getOrElse { return@launch onDone(Result.failure(it)) }
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

    /** Sign out everywhere. */
    fun signOutEverywhere() = viewModelScope.launch {
        keys.delete()
        store.reset()
        mutableSession.value = SessionState()
    }
}
