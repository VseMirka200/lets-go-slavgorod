package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.ui.viewmodel.themeDataStore
import com.example.lets_go_slavgorod.ui.viewmodel.displayDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * ViewModel для управления данными приложения
 * 
 * Версия: 2.0
 * Последнее обновление: Октябрь 2025
 * 
 * Основные функции:
 * - Сброс настроек к значениям по умолчанию
 * - Обновление расписания из GitHub
 * - Проверка доступности обновлений расписания
 * - Очистка кэша приложения
 * 
 * Изменения v2.0:
 * - Добавлена поддержка удалённой загрузки расписания
 * - Добавлены методы для ручного обновления данных
 * - Добавлена проверка версии данных
 * 
 * @param context контекст приложения
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
class DataManagementViewModel(private val context: Context) : ViewModel() {

    private val repository = BusRouteRepository(context)
    
    // Состояния для обновления расписания
    private val _isRefreshingSchedule = MutableStateFlow(false)
    val isRefreshingSchedule: StateFlow<Boolean> = _isRefreshingSchedule.asStateFlow()
    
    private val _scheduleRefreshError = MutableStateFlow<String?>(null)
    val scheduleRefreshError: StateFlow<String?> = _scheduleRefreshError.asStateFlow()
    
    private val _scheduleRefreshSuccess = MutableStateFlow(false)
    val scheduleRefreshSuccess: StateFlow<Boolean> = _scheduleRefreshSuccess.asStateFlow()
    
    private val _dataVersion = MutableStateFlow<String?>(null)
    val dataVersion: StateFlow<String?> = _dataVersion.asStateFlow()
    
    private val _dataLastUpdated = MutableStateFlow<String?>(null)
    val dataLastUpdated: StateFlow<String?> = _dataLastUpdated.asStateFlow()
    
    // Состояние доступности обновления расписания (для Badge)
    private val _scheduleUpdateAvailable = MutableStateFlow(false)
    val scheduleUpdateAvailable: StateFlow<Boolean> = _scheduleUpdateAvailable.asStateFlow()
    
    init {
        // Загружаем информацию о версии данных
        viewModelScope.launch {
            try {
                _dataVersion.value = repository.getDataVersion()
                _dataLastUpdated.value = repository.getDataLastUpdated()
                
                // Проверяем доступность обновлений (без задержки, т.к. это ViewModel для настроек)
                val hasUpdates = repository.checkForDataUpdates()
                _scheduleUpdateAvailable.value = hasUpdates
                Timber.d("DataManagementViewModel init: schedule update available = $hasUpdates")
            } catch (e: Exception) {
                Timber.e(e, "Error loading data version info")
            }
        }
    }
    
    /**
     * Обновляет расписание из GitHub (Улучшенная версия без перезапуска)
     * 
     * Загружает актуальную версию routes_data.json с GitHub,
     * обновляет локальный кэш и автоматически обновляет UI через StateFlow.
     * 
     * Изменения v2.1:
     * - Убран перезапуск приложения
     * - Реактивное обновление через StateFlow
     * - Улучшенный UX с подробными статусами
     */
    fun refreshScheduleFromGitHub() {
        viewModelScope.launch {
            try {
                _isRefreshingSchedule.value = true
                _scheduleRefreshError.value = null
                _scheduleRefreshSuccess.value = false
                
                Timber.i("User initiated schedule refresh from GitHub")
                
                val success = repository.refreshRoutesFromRemote()
                
                if (success) {
                    _scheduleRefreshSuccess.value = true
                    _scheduleRefreshError.value = null
                    
                    // Обновляем версию данных
                    _dataVersion.value = repository.getDataVersion()
                    _dataLastUpdated.value = repository.getDataLastUpdated()
                    
                    // Сбрасываем флаг доступности обновления
                    _scheduleUpdateAvailable.value = false
                    
                    Timber.i("Schedule successfully refreshed from GitHub")
                    Timber.i("UI will update automatically via StateFlow - no restart needed!")
                    
                    // UI автоматически обновится через StateFlow в BusViewModel
                    // Перезапуск НЕ НУЖЕН - данные обновляются реактивно ✅
                } else {
                    _scheduleRefreshError.value = "Не удалось загрузить данные с сервера"
                    Timber.w("Failed to refresh schedule from GitHub")
                }
            } catch (e: Exception) {
                _scheduleRefreshError.value = e.message ?: "Неизвестная ошибка"
                Timber.e(e, "Error refreshing schedule from GitHub")
            } finally {
                _isRefreshingSchedule.value = false
            }
        }
    }
    
    /**
     * Проверяет доступность обновлений расписания
     * 
     * @return true если доступна новая версия
     */
    suspend fun checkForScheduleUpdates(): Boolean {
        return try {
            val hasUpdates = repository.checkForDataUpdates()
            _scheduleUpdateAvailable.value = hasUpdates
            hasUpdates
        } catch (e: Exception) {
            Timber.e(e, "Error checking for schedule updates")
            false
        }
    }
    
    /**
     * Сбрасывает флаг доступности обновления расписания
     * 
     * Вызывается после успешного обновления расписания
     */
    fun clearScheduleUpdateAvailable() {
        _scheduleUpdateAvailable.value = false
    }
    
    /**
     * Очищает статус обновления расписания
     */
    fun clearScheduleRefreshStatus() {
        _scheduleRefreshSuccess.value = false
        _scheduleRefreshError.value = null
    }

    /**
     * Сброс всех настроек к значениям по умолчанию
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            try {
                Timber.d("=== Starting reset of all settings ===")
                
                withContext(Dispatchers.IO) {
                    // Очищаем все DataStore
                    try {
                        context.dataStore.edit { it.clear() }
                        Timber.d("Main DataStore cleared")
                    } catch (e: Exception) {
                        Timber.e(e, "Error clearing main DataStore")
                    }
                    
                    try {
                        context.themeDataStore.edit { it.clear() }
                        Timber.d("Theme DataStore cleared")
                    } catch (e: Exception) {
                        Timber.e(e, "Error clearing theme DataStore")
                    }
                    
                    try {
                        context.displayDataStore.edit { it.clear() }
                        Timber.d("Display DataStore cleared")
                    } catch (e: Exception) {
                        Timber.e(e, "Error clearing display DataStore")
                    }
                    
                    // Удаляем файлы DataStore напрямую для полной очистки
                    try {
                        val dataStoreDir = File(context.filesDir, "datastore")
                        if (dataStoreDir.exists() && dataStoreDir.isDirectory) {
                            val files = dataStoreDir.listFiles()
                            Timber.d("Found ${files?.size ?: 0} DataStore files to delete")
                            files?.forEach { file ->
                                if (file.delete()) {
                                    Timber.d("Deleted: ${file.name}")
                                } else {
                                    Timber.w("Failed to delete: ${file.name}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error deleting DataStore files")
                    }
                    
                    // Очищаем SharedPreferences (если есть)
                    try {
                        val prefsDir = File(context.filesDir.parent, "shared_prefs")
                        if (prefsDir.exists() && prefsDir.isDirectory) {
                            val files = prefsDir.listFiles()
                            Timber.d("Found ${files?.size ?: 0} SharedPreferences files to delete")
                            files?.forEach { file ->
                                if (file.delete()) {
                                    Timber.d("Deleted: ${file.name}")
                                } else {
                                    Timber.w("Failed to delete: ${file.name}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error deleting SharedPreferences files")
                    }
                }
                
                Timber.d("=== All settings cleared, restarting app ===")
                
                // Даем время на завершение всех операций
                kotlinx.coroutines.delay(com.example.lets_go_slavgorod.utils.Constants.DATA_OPERATION_COMPLETION_DELAY_MS)
                
                // Перезапускаем приложение
                withContext(Dispatchers.Main) {
                    restartApp()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Critical error resetting settings")
            }
        }
    }
    
    /**
     * Перезапускает приложение (используется только при сбросе настроек)
     * 
     * Примечание: Обновление расписания больше НЕ требует перезапуска,
     * т.к. данные обновляются реактивно через StateFlow.
     */
    private fun restartApp() {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (intent != null) {
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                context.startActivity(intent)
                
                // Завершаем текущий процесс
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(0)
            } else {
                Timber.e("Failed to get launch intent for restart")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error restarting app")
            // Принудительный выход
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

}


