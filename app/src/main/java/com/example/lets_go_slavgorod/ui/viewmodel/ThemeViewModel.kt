package com.example.lets_go_slavgorod.ui.viewmodel

// Android системные импорты
import android.content.Context

// DataStore импорты
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

// ViewModel импорты
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

// Coroutines импорты
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * DataStore для хранения настроек темы приложения
 */
val Context.themeDataStore by preferencesDataStore(name = "theme_preferences")

/**
 * Перечисление доступных тем приложения
 * 
 * @property SYSTEM следовать системной теме
 * @property LIGHT светлая тема
 * @property DARK темная тема
 */
enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

/**
 * ViewModel для управления темой приложения
 * 
 * Основные функции:
 * - Сохранение и загрузка настроек темы
 * - Управление переключением между темами
 * - Интеграция с DataStore для персистентности
 * 
 * @param dataStore DataStore для хранения настроек
 * 
 * @author VseMirka200
 * @version 1.0
 */
class ThemeViewModel(private val dataStore: DataStore<Preferences>) : ViewModel() {

    /**
     * Ключи для хранения настроек в DataStore
     */
    private object PreferencesKeys {
        val APP_THEME = stringPreferencesKey("app_theme")
    }

    val currentTheme = dataStore.data
        .map { preferences ->
            AppTheme.valueOf(preferences[PreferencesKeys.APP_THEME] ?: AppTheme.SYSTEM.name)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = AppTheme.SYSTEM
        )

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.APP_THEME] = theme.name
            }
        }
    }
}
