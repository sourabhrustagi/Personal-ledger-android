package com.ledger.personal.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ledger.personal.data.security.PinSealer
import com.ledger.personal.data.security.pinMatches
import com.ledger.personal.data.security.sealPin
import com.ledger.personal.ui.screens.create.DEFAULT_CATEGORIES
import com.ledger.personal.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AppPrefs(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    val phone: String = "",
    val name: String = "",
    val mpinHash: String = "",
    val mpinSalt: String = "",
    val biometric: Boolean = false,
    val hideBalances: Boolean = true,
    val language: String = "English",
    val categories: List<String> = DEFAULT_CATEGORIES,
    val deviceId: String = "",
)

private val Context.appStore: DataStore<Preferences> by preferencesDataStore(name = "ledgerone_prefs")

private val THEME = stringPreferencesKey("theme")
private val ONBOARDED = booleanPreferencesKey("onboarded")
private val PHONE = stringPreferencesKey("phone")
private val NAME = stringPreferencesKey("name")
private val MPIN_HASH = stringPreferencesKey("mpin_hash")
private val MPIN_SALT = stringPreferencesKey("mpin_salt")
private val BIOMETRIC = booleanPreferencesKey("biometric")
private val HIDE_BALANCES = booleanPreferencesKey("hide_balances")
private val LANGUAGE = stringPreferencesKey("language")
private val CATEGORIES = stringPreferencesKey("categories")
private val DEVICE_ID = stringPreferencesKey("device_id")
private const val SEP = "\u001F"

@Singleton
class AppPreferences @Inject constructor(@ApplicationContext private val context: Context) {
    val prefs: Flow<AppPrefs> = context.appStore.data.map { p ->
        AppPrefs(
            theme = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            onboarded = p[ONBOARDED] ?: false,
            phone = p[PHONE].orEmpty(),
            name = p[NAME].orEmpty(),
            mpinHash = p[MPIN_HASH].orEmpty(),
            mpinSalt = p[MPIN_SALT].orEmpty(),
            biometric = p[BIOMETRIC] ?: false,
            hideBalances = p[HIDE_BALANCES] ?: true,
            language = p[LANGUAGE] ?: "English",
            categories = p[CATEGORIES]?.split(SEP)?.filter { it.isNotBlank() } ?: DEFAULT_CATEGORIES,
            deviceId = p[DEVICE_ID].orEmpty(),
        )
    }

    fun matches(pin: String, prefs: AppPrefs): Boolean = pinMatches(pin, prefs.mpinSalt, prefs.mpinHash)

    suspend fun completeOnboarding(name: String, phone: String, pin: String, biometric: Boolean, language: String) {
        val (salt, hash) = sealPin(pin)
        context.appStore.edit {
            it[NAME] = name
            it[PHONE] = phone
            it[MPIN_SALT] = salt
            it[MPIN_HASH] = hash
            it[BIOMETRIC] = biometric
            it[LANGUAGE] = language
            it[ONBOARDED] = true
        }
    }

    suspend fun setTheme(mode: ThemeMode) = context.appStore.edit { it[THEME] = mode.name }

    suspend fun setBiometric(enabled: Boolean) = context.appStore.edit { it[BIOMETRIC] = enabled }

    suspend fun setHideBalances(hide: Boolean) = context.appStore.edit { it[HIDE_BALANCES] = hide }

    suspend fun changeMpin(pin: String) {
        val (salt, hash) = sealPin(pin)
        context.appStore.edit {
            it[MPIN_SALT] = salt
            it[MPIN_HASH] = hash
        }
    }

    suspend fun setCategories(names: List<String>) = context.appStore.edit { it[CATEGORIES] = names.joinToString(SEP) }

    suspend fun setDeviceId(id: String) = context.appStore.edit { it[DEVICE_ID] = id }

    suspend fun reset() {
        PinSealer.delete()
        context.appStore.edit { it.clear() }
    }
}
