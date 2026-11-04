package ru.slavgorod.transport.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import ru.slavgorod.transport.core.Constants

class ScheduleSourceSettings(private val dataStore: DataStore<Preferences>) {
    val sourceUrl: Flow<String> = dataStore.data.map { preferences ->
        preferences[PreferencesKeys.remoteJsonUrl]?.takeIf(::isValidScheduleSourceUrl)
            ?: Constants.REMOTE_JSON_URL
    }

    suspend fun currentUrl(): String = sourceUrl.first()

    suspend fun save(url: String) {
        val value = url.trim()
        require(isValidScheduleSourceUrl(value))
        dataStore.edit { it[PreferencesKeys.remoteJsonUrl] = value }
    }

    suspend fun reset() {
        dataStore.edit { it.remove(PreferencesKeys.remoteJsonUrl) }
    }
}

internal fun isValidScheduleSourceUrl(value: String): Boolean {
    val url = value.trim().toHttpUrlOrNull() ?: return false
    return url.isHttps && url.username.isEmpty() && url.password.isEmpty() && url.fragment == null
}
