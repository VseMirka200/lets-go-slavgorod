package ru.slavgorod.transport.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.slavgorod.transport.notifications.hasPostNotificationsPermission

class ScheduleNotificationSettings(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {

    val enabled: StateFlow<Boolean> = dataStore.stateInPreferences(
        scope = scope,
        initialValue = false
    ) { preferences ->
        preferences[PreferencesKeys.scheduleNotificationsEnabled] ?: false
    }

    fun setEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.scheduleNotificationsEnabled] = enabled
            }
        }
    }

    suspend fun isEnabledNow(): Boolean {
        return dataStore.data.first()[PreferencesKeys.scheduleNotificationsEnabled] ?: false
    }

    suspend fun syncPermissionState(context: Context) {
        if (isEnabledNow() && !context.hasPostNotificationsPermission()) {
            setEnabled(false)
        }
    }

}
