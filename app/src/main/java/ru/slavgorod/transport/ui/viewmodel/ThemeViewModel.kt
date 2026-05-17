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
import ru.slavgorod.transport.ui.model.AppTheme

class ThemeViewModel(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    val currentTheme: StateFlow<AppTheme> = dataStore.stateInPreferences(
        scope = viewModelScope,
        initialValue = AppTheme.SYSTEM
    ) { preferences ->
        preferences[PreferencesKeys.appTheme].toEnumOrDefault(AppTheme.SYSTEM)
    }

    fun setTheme(theme: AppTheme) {
        UserActionLogger.preferenceChanged(R.string.settings_theme_title, theme.name)
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.appTheme] = theme.name
            }
        }
    }

}
