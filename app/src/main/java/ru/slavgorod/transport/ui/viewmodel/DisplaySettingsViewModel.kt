package ru.slavgorod.transport.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.slavgorod.transport.R
import ru.slavgorod.transport.data.local.PreferencesKeys
import ru.slavgorod.transport.data.local.stateInPreferences
import ru.slavgorod.transport.data.local.toEnumOrDefault
import ru.slavgorod.transport.logging.UserActionLogger
import ru.slavgorod.transport.ui.model.ScheduleDaySelectionMode

/**
 * Хранит настройки отображения расписания в DataStore.
 *
 * Подсказка разработчику: перед записью нормализуйте значения настроек, чтобы старые
 * или повреждённые данные не создавали недопустимую конфигурацию интерфейса.
 */
class DisplaySettingsViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    /** Число колонок в диапазоне от 1 до 4; до загрузки настроек используется 2. */
    val gridColumns: StateFlow<Int> = dataStore.stateInPreferences(
        scope = viewModelScope,
        initialValue = DEFAULT_GRID_COLUMNS
    ) { preferences ->
        (preferences[PreferencesKeys.gridColumns] ?: DEFAULT_GRID_COLUMNS)
            .coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    /** Сохранённый порядок колонок расписания; до загрузки используется `false`. */
    val swapScheduleColumns: StateFlow<Boolean> = dataStore.stateInPreferences(
        scope = viewModelScope,
        initialValue = DEFAULT_SWAP_SCHEDULE_COLUMNS
    ) { preferences ->
        preferences[PreferencesKeys.swapScheduleColumns] ?: DEFAULT_SWAP_SCHEDULE_COLUMNS
    }

    /** Режим выбора дня; неизвестное сохранённое значение заменяется режимом по умолчанию. */
    val scheduleDaySelectionMode: StateFlow<ScheduleDaySelectionMode> = dataStore.stateInPreferences(
        scope = viewModelScope,
        initialValue = DEFAULT_SCHEDULE_DAY_SELECTION_MODE
    ) { preferences ->
        preferences[PreferencesKeys.scheduleDaySelectionMode]
            .toEnumOrDefault(DEFAULT_SCHEDULE_DAY_SELECTION_MODE)
    }

    /** Ограничивает число колонок допустимым диапазоном и запускает сохранение. */
    fun setGridColumns(columns: Int) {
        val normalizedColumns = columns.coerceGridColumns()
        UserActionLogger.preferenceChanged(
            R.string.settings_columns_title,
            normalizedColumns.toString()
        )
        persistDisplaySettings(columns = normalizedColumns)
    }

    /** Запускает сохранение выбранного режима в области жизненного цикла ViewModel. */
    fun setScheduleDaySelectionMode(mode: ScheduleDaySelectionMode) {
        UserActionLogger.preferenceChanged(R.string.settings_schedule_day_mode_title, mode.name)
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.scheduleDaySelectionMode] = mode.name
            }
        }
    }

    private fun persistDisplaySettings(
        columns: Int = gridColumns.value,
        swapScheduleColumns: Boolean = this.swapScheduleColumns.value
    ) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.gridColumns] = columns
                preferences[PreferencesKeys.swapScheduleColumns] = swapScheduleColumns
            }
        }
    }

    private fun Int.coerceGridColumns(): Int {
        return coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    private companion object {
        private const val MIN_GRID_COLUMNS = 1
        private const val MAX_GRID_COLUMNS = 4
        private const val DEFAULT_GRID_COLUMNS = 2
        private const val DEFAULT_SWAP_SCHEDULE_COLUMNS = false
        private val DEFAULT_SCHEDULE_DAY_SELECTION_MODE = ScheduleDaySelectionMode.Default
    }
}
