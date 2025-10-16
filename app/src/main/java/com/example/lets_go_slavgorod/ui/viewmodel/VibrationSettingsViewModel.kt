package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel для управления настройками вибрации при получении уведомлений
 * 
 * Управляет включением/выключением вибрации устройства при срабатывании
 * уведомлений о предстоящем отправлении автобуса.
 * 
 * Основные функции:
 * - Чтение текущего состояния настройки вибрации из DataStore
 * - Изменение настройки с сохранением в DataStore
 * - Обновление кэша настроек для быстрого доступа без корутин
 * 
 * Особенности:
 * - По умолчанию вибрация включена
 * - Настройка применяется ко всем уведомлениям
 * - Изменения сохраняются в NotificationPreferencesCache для синхронного доступа
 * 
 * @param context контекст для доступа к DataStore
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
class VibrationSettingsViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        /** Ключ для хранения настройки вибрации в DataStore */
        private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
    }
    
    /**
     * Текущее состояние настройки вибрации
     * 
     * StateFlow автоматически обновляется при изменении настроек в DataStore.
     * Используется в UI для отображения текущего состояния переключателя.
     * 
     * @see setVibrationEnabled
     */
    val vibrationEnabled: StateFlow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[VIBRATION_ENABLED_KEY] ?: true  // По умолчанию включено
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    /**
     * Устанавливает состояние вибрации для уведомлений
     * 
     * Сохраняет новое значение в DataStore и обновляет кэш настроек.
     * Кэш необходим для синхронного доступа из BroadcastReceiver,
     * где нельзя использовать suspend функции.
     * 
     * @param enabled true для включения вибрации, false для выключения
     */
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                // Сохраняем в DataStore
                context.dataStore.edit { preferences ->
                    preferences[VIBRATION_ENABLED_KEY] = enabled
                }
                Timber.d("Vibration settings updated: enabled=$enabled")
                
                // ВАЖНО: Обновляем кэш настроек для синхронного доступа
                // Это необходимо для AlarmReceiver, который не может использовать suspend функции
                NotificationPreferencesCache.updateCache(context)
            } catch (e: Exception) {
                Timber.e(e, "Error saving vibration settings")
            }
        }
    }
}

