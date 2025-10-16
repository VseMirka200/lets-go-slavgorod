package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * DataStore для хранения настроек отображения маршрутов
 */
val Context.displayDataStore: DataStore<Preferences> by preferencesDataStore(name = "display_settings")

/**
 * Режим отображения списка маршрутов
 * 
 * Определяет как маршруты отображаются на экранах:
 * - GRID: режим сетки с настраиваемым количеством колонок (1-4)
 * - LIST: режим списка с детальной информацией
 */
enum class RouteDisplayMode {
    /** Отображение в виде сетки (grid) */
    GRID,
    
    /** Отображение в виде списка (list) */
    LIST
}

/**
 * ViewModel для управления настройками отображения маршрутов
 * 
 * Управляет пользовательскими предпочтениями по отображению списка маршрутов:
 * - Режим отображения (сетка или список)
 * - Количество колонок для режима сетки (1-4)
 * 
 * Все настройки сохраняются в DataStore и переживают перезапуск приложения.
 * Изменения автоматически применяются ко всем экранам с маршрутами.
 * 
 * @param context контекст для доступа к DataStore
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
class DisplaySettingsViewModel(
    private val context: Context
) : ViewModel() {

    /** Текущий режим отображения маршрутов */
    private val _displayMode = MutableStateFlow(RouteDisplayMode.GRID)
    val displayMode: Flow<RouteDisplayMode> = _displayMode.asStateFlow()
    
    /** Текущее количество колонок для режима сетки (1-4) */
    private val _gridColumns = MutableStateFlow(2)
    val gridColumns: Flow<Int> = _gridColumns.asStateFlow()

    init {
        // Загружаем сохраненные настройки при создании ViewModel
        loadDisplaySettings()
    }

    /**
     * Загружает настройки отображения из DataStore
     * 
     * Подписывается на изменения в DataStore и автоматически обновляет
     * локальное состояние при изменении сохраненных настроек.
     */
    private fun loadDisplaySettings() {
        viewModelScope.launch {
            context.displayDataStore.data.collect { preferences ->
                // Загружаем режим отображения (по умолчанию сетка)
                val isGridMode = preferences[DISPLAY_MODE_GRID] ?: true
                _displayMode.value = if (isGridMode) RouteDisplayMode.GRID else RouteDisplayMode.LIST
                
                // Загружаем количество колонок (по умолчанию 2, ограничено 1-4)
                val columns = preferences[GRID_COLUMNS] ?: 2
                _gridColumns.value = columns.coerceIn(1, 4)
            }
        }
    }

    /**
     * Устанавливает режим отображения маршрутов
     * 
     * Изменение сохраняется в DataStore и применяется ко всем экранам.
     * 
     * @param mode новый режим отображения (GRID или LIST)
     */
    fun setDisplayMode(mode: RouteDisplayMode) {
        viewModelScope.launch {
            _displayMode.value = mode
            context.displayDataStore.edit { preferences ->
                preferences[DISPLAY_MODE_GRID] = (mode == RouteDisplayMode.GRID)
            }
        }
    }
    
    /**
     * Устанавливает количество колонок для режима сетки
     * 
     * Значение автоматически ограничивается диапазоном 1-4.
     * Применяется только в режиме GRID.
     * 
     * @param columns количество колонок (1-4)
     */
    fun setGridColumns(columns: Int) {
        viewModelScope.launch {
            val validColumns = columns.coerceIn(1, 4)
            _gridColumns.value = validColumns
            context.displayDataStore.edit { preferences ->
                preferences[GRID_COLUMNS] = validColumns
            }
        }
    }

    companion object {
        /** Ключ для хранения режима отображения в DataStore */
        private val DISPLAY_MODE_GRID = booleanPreferencesKey("display_mode_grid")
        
        /** Ключ для хранения количества колонок в DataStore */
        private val GRID_COLUMNS = intPreferencesKey("grid_columns")
    }
}
