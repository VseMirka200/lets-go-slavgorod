/**
 * Настройки обновлений приложения
 * 
 * Управляет сохранением и получением настроек обновлений:
 * - Режим обновлений (автоматический/ручной/отключено)
 * - Время последней проверки обновлений
 * - Информация о доступном обновлении
 */
package com.example.lets_go_slavgorod.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore для настроек обновлений
 */
private val Context.updatePreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "update_preferences")

/**
 * Ключи для настроек обновлений
 */
private object UpdatePreferencesKeys {
    val AUTO_UPDATE_CHECK_ENABLED = booleanPreferencesKey("auto_update_check_enabled")
    val LAST_UPDATE_CHECK_TIME = longPreferencesKey("last_update_check_time")
    val AVAILABLE_UPDATE_VERSION = stringPreferencesKey("available_update_version")
    val AVAILABLE_UPDATE_URL = stringPreferencesKey("available_update_url")
    val AVAILABLE_UPDATE_NOTES = stringPreferencesKey("available_update_notes")
}

/**
 * Класс для управления настройками обновлений
 */
class UpdatePreferences(private val context: Context) {
    
    /**
     * Поток состояния автоматической проверки обновлений
     */
    val autoUpdateCheckEnabled: Flow<Boolean> = context.updatePreferencesDataStore.data
        .map { preferences ->
            preferences[UpdatePreferencesKeys.AUTO_UPDATE_CHECK_ENABLED] ?: true
        }
    
    /**
     * Поток времени последней проверки обновлений
     */
    val lastUpdateCheckTime: Flow<Long> = context.updatePreferencesDataStore.data
        .map { preferences ->
            preferences[UpdatePreferencesKeys.LAST_UPDATE_CHECK_TIME] ?: 0L
        }
    
    /**
     * Поток версии доступного обновления
     */
    val availableUpdateVersion: Flow<String?> = context.updatePreferencesDataStore.data
        .map { preferences ->
            preferences[UpdatePreferencesKeys.AVAILABLE_UPDATE_VERSION]
        }
    
    /**
     * Поток URL для скачивания обновления
     */
    val availableUpdateUrl: Flow<String?> = context.updatePreferencesDataStore.data
        .map { preferences ->
            preferences[UpdatePreferencesKeys.AVAILABLE_UPDATE_URL]
        }
    
    /**
     * Поток описания изменений в обновлении
     */
    val availableUpdateNotes: Flow<String?> = context.updatePreferencesDataStore.data
        .map { preferences ->
            preferences[UpdatePreferencesKeys.AVAILABLE_UPDATE_NOTES]
        }
    
    /**
     * Устанавливает состояние автоматической проверки обновлений
     */
    suspend fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        context.updatePreferencesDataStore.edit { preferences ->
            preferences[UpdatePreferencesKeys.AUTO_UPDATE_CHECK_ENABLED] = enabled
        }
    }
    
    /**
     * Устанавливает время последней проверки обновлений
     */
    suspend fun setLastUpdateCheckTime(time: Long) {
        context.updatePreferencesDataStore.edit { preferences ->
            preferences[UpdatePreferencesKeys.LAST_UPDATE_CHECK_TIME] = time
        }
    }
    
    /**
     * Сохраняет информацию о доступном обновлении
     */
    suspend fun setAvailableUpdate(version: String, url: String, notes: String) {
        context.updatePreferencesDataStore.edit { preferences ->
            preferences[UpdatePreferencesKeys.AVAILABLE_UPDATE_VERSION] = version
            preferences[UpdatePreferencesKeys.AVAILABLE_UPDATE_URL] = url
            preferences[UpdatePreferencesKeys.AVAILABLE_UPDATE_NOTES] = notes
        }
    }
    
    /**
     * Очищает информацию о доступном обновлении
     */
    suspend fun clearAvailableUpdate() {
        context.updatePreferencesDataStore.edit { preferences ->
            preferences.remove(UpdatePreferencesKeys.AVAILABLE_UPDATE_VERSION)
            preferences.remove(UpdatePreferencesKeys.AVAILABLE_UPDATE_URL)
            preferences.remove(UpdatePreferencesKeys.AVAILABLE_UPDATE_NOTES)
        }
    }
}
