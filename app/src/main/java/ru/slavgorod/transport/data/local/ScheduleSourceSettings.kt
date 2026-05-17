package ru.slavgorod.transport.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import ru.slavgorod.transport.core.ScheduleConfig

class ScheduleSourceSettings(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {

    val remoteJsonUrl: StateFlow<String> = dataStore.stateInPreferences(
        scope = scope,
        initialValue = ScheduleConfig.remoteJsonUrl
    ) { preferences ->
        preferences.remoteJsonUrlOrDefault()
    }

    suspend fun getRemoteJsonUrl(): String {
        return dataStore.data.first().remoteJsonUrlOrDefault()
    }

    suspend fun setRemoteJsonUrl(url: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.remoteJsonUrl] = url.trim()
        }
    }

    suspend fun resetRemoteJsonUrl() {
        dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.remoteJsonUrl)
        }
    }
}

private fun Preferences.remoteJsonUrlOrDefault(): String {
    return this[PreferencesKeys.remoteJsonUrl]
        ?.trim()
        ?.takeIf(String::isNotBlank)
        ?: ScheduleConfig.remoteJsonUrl
}
