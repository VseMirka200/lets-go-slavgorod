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

/** Хранит пользовательскую настройку уведомлений об изменении расписания. */
class ScheduleNotificationSettings(
    private val dataStore: DataStore<Preferences>,
    private val scope: CoroutineScope
) {

    /** Последнее наблюдаемое значение; до чтения настроек и при их отсутствии — `false`. */
    val enabled: StateFlow<Boolean> = dataStore.stateInPreferences(
        scope = scope,
        initialValue = false
    ) { preferences ->
        preferences[PreferencesKeys.scheduleNotificationsEnabled] ?: false
    }

    /** Запускает сохранение в переданной области корутин, не ожидая завершения записи. */
    fun setEnabled(enabled: Boolean) {
        scope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.scheduleNotificationsEnabled] = enabled
            }
        }
    }

    /** Читает значение непосредственно из DataStore, по умолчанию возвращает `false`. */
    suspend fun isEnabledNow(): Boolean {
        return dataStore.data.first()[PreferencesKeys.scheduleNotificationsEnabled] ?: false
    }

    /** При отсутствии системного разрешения запускает отключение сохранённой настройки. */
    suspend fun syncPermissionState(context: Context) {
        if (isEnabledNow() && !context.hasPostNotificationsPermission()) {
            setEnabled(false)
        }
    }

}
