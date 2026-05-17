package ru.slavgorod.transport.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.slavgorod.transport.core.Constants

val Context.dataStore by preferencesDataStore(name = "settings")

val Context.displayDataStore: DataStore<Preferences> by preferencesDataStore(name = "display_settings")

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

object PreferencesKeys {
    val gridColumns = intPreferencesKey("grid_columns")
    val swapScheduleColumns = booleanPreferencesKey("swap_schedule_columns")
    val scheduleNotificationsEnabled = booleanPreferencesKey("schedule_notifications_enabled")
    val appTheme = stringPreferencesKey("app_theme")
    val routeOrder = stringPreferencesKey("route_order")
    val pinnedRouteIds = stringPreferencesKey("pinned_route_ids")
    val remoteJsonUrl = stringPreferencesKey("remote_json_url")
}

fun <T> DataStore<Preferences>.stateInPreferences(
    scope: CoroutineScope,
    initialValue: T,
    started: SharingStarted = SharingStarted.WhileSubscribed(Constants.STATE_FLOW_TIMEOUT_MS),
    mapper: (Preferences) -> T
): StateFlow<T> {
    return data
        .map(mapper)
        .stateIn(
            scope = scope,
            started = started,
            initialValue = initialValue
        )
}

inline fun <reified T : Enum<T>> String?.toEnumOrDefault(fallback: T): T {
    val rawValue = takeUnless { it.isNullOrBlank() } ?: return fallback
    return enumValues<T>().firstOrNull { it.name == rawValue } ?: fallback
}

private val Context.disclaimerDataStore: DataStore<Preferences> by
preferencesDataStore(name = "disclaimer_preferences")

object DisclaimerManager {

    private object Keys {
        val disclaimerShown = booleanPreferencesKey("disclaimer_shown")
    }

    suspend fun shouldShowDisclaimer(context: Context): Boolean {
        return !context.disclaimerDataStore.data
            .map { preferences -> preferences[Keys.disclaimerShown] ?: false }
            .first()
    }

    suspend fun markDisclaimerAccepted(context: Context) {
        context.disclaimerDataStore.edit { preferences ->
            preferences[Keys.disclaimerShown] = true
        }
    }

    suspend fun clearState(context: Context) {
        context.disclaimerDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
