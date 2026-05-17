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
import ru.slavgorod.transport.logging.UserActionLogger

class DisplaySettingsViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val gridColumns: StateFlow<Int> = dataStore.stateInPreferences(
        scope = viewModelScope,
        initialValue = DEFAULT_GRID_COLUMNS
    ) { preferences ->
        (preferences[PreferencesKeys.gridColumns] ?: DEFAULT_GRID_COLUMNS)
            .coerceIn(MIN_GRID_COLUMNS, MAX_GRID_COLUMNS)
    }

    val swapScheduleColumns: StateFlow<Boolean> = dataStore.stateInPreferences(
        scope = viewModelScope,
        initialValue = DEFAULT_SWAP_SCHEDULE_COLUMNS
    ) { preferences ->
        preferences[PreferencesKeys.swapScheduleColumns] ?: DEFAULT_SWAP_SCHEDULE_COLUMNS
    }

    fun setGridColumns(columns: Int) {
        val normalizedColumns = columns.coerceGridColumns()
        UserActionLogger.preferenceChanged(
            R.string.settings_columns_title,
            normalizedColumns.toString()
        )
        persistDisplaySettings(columns = normalizedColumns)
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
    }
}
