package com.ledger.personal.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ledger.personal.domain.settings.UserSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val SETTINGS_NAME = "ledger_settings"
private const val LEGACY_PREFS = "ledger_prefs"
private val LAST_SYNC = longPreferencesKey("last_sync_epoch_ms")

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, LEGACY_PREFS))
    },
)

@Singleton
class DataStoreUserSettings @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserSettings {
    override val lastSyncEpochMs: Flow<Long?> = context.settingsDataStore.data.map { prefs ->
        prefs[LAST_SYNC]
    }

    override suspend fun recordSync(epochMs: Long) {
        context.settingsDataStore.edit { prefs -> prefs[LAST_SYNC] = epochMs }
    }
}
