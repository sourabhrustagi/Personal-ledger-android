package com.ledger.personal.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ledger.personal.data.security.PinSealer
import com.ledger.personal.data.security.pinMatches
import com.ledger.personal.data.security.sealPin
import com.ledger.personal.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** App prefs. */
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
    val categories: List<String> = emptyList(),
    val defaultCategories: List<String> = emptyList(),
    val deviceId: String = "",
    val ignoredUpdateCode: Int = 0,
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
private val DEFAULT_CATEGORY_KEY = stringPreferencesKey("default_categories")
private val DEVICE_ID = stringPreferencesKey("device_id")
private val IGNORED_UPDATE = intPreferencesKey("ignored_update")
private const val SEP = "\u001F"

/** App preferences. */
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
            categories = p[CATEGORIES]?.split(SEP)?.filter { it.isNotBlank() }.orEmpty(),
            defaultCategories = p[DEFAULT_CATEGORY_KEY]?.split(SEP)?.filter { it.isNotBlank() }.orEmpty(),
            deviceId = p[DEVICE_ID].orEmpty(),
            ignoredUpdateCode = p[IGNORED_UPDATE] ?: 0,
        )
    }

    /** Matches. */
    fun matches(pin: String, prefs: AppPrefs): Boolean = pinMatches(pin, prefs.mpinSalt, prefs.mpinHash)

    /** Finishes onboarding. */
    suspend fun completeOnboarding(name: String, phone: String, pin: String, biometric: Boolean, language: String) {
        val sealed = sealPin(pin)
        context.appStore.edit {
            it[NAME] = name
            it[PHONE] = phone
            it[MPIN_SALT] = sealed.saltHex
            it[MPIN_HASH] = sealed.verifier
            it[BIOMETRIC] = biometric
            it[LANGUAGE] = language
            it[ONBOARDED] = true
        }
    }

    /** Stores theme. */
    suspend fun setTheme(mode: ThemeMode) = context.appStore.edit { it[THEME] = mode.name }

    /** Stores biometric. */
    suspend fun setBiometric(enabled: Boolean) = context.appStore.edit { it[BIOMETRIC] = enabled }

    /** Stores hide balances. */
    suspend fun setHideBalances(hide: Boolean) = context.appStore.edit { it[HIDE_BALANCES] = hide }

    /** Changes mpin. */
    suspend fun changeMpin(pin: String) {
        val sealed = sealPin(pin)
        context.appStore.edit {
            it[MPIN_SALT] = sealed.saltHex
            it[MPIN_HASH] = sealed.verifier
        }
    }

    /** Stores categories. */
    suspend fun setCategories(names: List<String>) = context.appStore.edit { it[CATEGORIES] = names.joinToString(SEP) }

    /** Null when the user has never saved a category list. */
    suspend fun storedCategories(): List<String>? = stored(CATEGORIES)

    /** Null until a static-catalog response has been saved. */
    suspend fun storedDefaults(): List<String>? = stored(DEFAULT_CATEGORY_KEY)

    /** Reads one saved name list, or null when that key has never been written. */
    private suspend fun stored(key: Preferences.Key<String>): List<String>? =
        context.appStore.data.first()[key]?.split(SEP)?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() }

    /** Saves the catalog from the static API and the list the app should show. */
    suspend fun saveCategorySync(defaults: List<String>, selected: List<String>) = context.appStore.edit {
        it[DEFAULT_CATEGORY_KEY] = defaults.joinToString(SEP)
        it[CATEGORIES] = selected.joinToString(SEP)
    }

    /** Stores device id. */
    suspend fun setDeviceId(id: String) = context.appStore.edit { it[DEVICE_ID] = id }

    /** Remembers a skipped soft-update versionCode. */
    suspend fun ignoreUpdate(versionCode: Int) = context.appStore.edit { it[IGNORED_UPDATE] = versionCode }

    /** Reset. */
    suspend fun reset() {
        PinSealer.delete()
        context.appStore.edit { it.clear() }
    }
}
