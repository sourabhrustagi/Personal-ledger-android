package com.ledger.personal.ui.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ledger.personal.data.prefs.AppPreferences
import com.ledger.personal.data.security.DeviceKeys
import com.ledger.personal.domain.session.Session
import com.ledger.personal.mock.MockValues
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Manages unlock verification and account reset for the locked/login screen.
 * Adheres to SRP by handling only user unlock authentication and device reset.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val store: AppPreferences,
    private val keys: DeviceKeys,
    private val session: Session,
) : ViewModel() {

    /** Verifies OTP to unlock / sign in. [onResult] runs after session is opened. */
    fun verifyOtp(code: String, onResult: (Boolean) -> Unit) {
        if (code != MockValues.OTP) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val result = session.begin()
            onResult(result.isSuccess)
        }
    }

    /** Clears session tokens, keystore keys, and local preferences to reset the app. */
    fun signOutEverywhere(onResetDone: () -> Unit = {}) {
        viewModelScope.launch {
            session.end()
            keys.delete()
            store.reset()
            onResetDone()
        }
    }
}
