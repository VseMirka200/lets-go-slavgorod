package com.example.lets_go_slavgorod.ui.viewmodel

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * ViewModel для управления данными приложения "Поехали! Славгород"
 * 
 * Предоставляет централизованное управление данными приложения:
 * - Сброс настроек к значениям по умолчанию
 * - Обновление расписания из удаленного источника (GitHub)
 * - Проверка доступности обновлений расписания
 * - Очистка кэша приложения и временных файлов
 * - Управление версиями данных
 * 
 * Архитектура:
 * - Использует Repository паттерн для работы с данными
 * - Асинхронные операции через Coroutines
 * - Реактивное состояние через StateFlow
 * - Интеграция с DataStore для настроек
 * - Поддержка удаленной загрузки через GitHub API
 * 
 * Основные функции:
 * - **Сброс настроек**: Восстановление всех настроек к значениям по умолчанию
 * - **Обновление данных**: Загрузка актуального расписания с GitHub
 * - **Проверка обновлений**: Автоматическая проверка доступности новых версий
 * - **Очистка кэша**: Освобождение дискового пространства
 * - **Управление версиями**: Отслеживание версий данных
 * 
 * Особенности:
 * - Thread-safe операции с данными
 * - Обработка ошибок сети и файловой системы
 * - Прогресс-индикаторы для длительных операций
 * - Автоматическое восстановление после ошибок
 * - Интеграция с системой логирования
 * 
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность работы с данными
 * - Обновлены комментарии и документация
 * - Добавлена поддержка системы логирования
 * - Улучшена обработка ошибок
 * 
 * @param context контекст приложения для доступа к файловой системе и DataStore
 * 
 * @author VseMirka200
 * @version 3.0
 * @since 2.0
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
            } catch (e: Exception) {
                Timber.e(e, "Ошибка загрузки информации о версии данных")
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
                
                
                val success = repository.refreshRoutesFromRemote()
                
                if (success) {
                    _scheduleRefreshSuccess.value = true
                    _scheduleRefreshError.value = null
                    
                    // Обновляем версию данных
                    _dataVersion.value = repository.getDataVersion()
                    _dataLastUpdated.value = repository.getDataLastUpdated()
                    
                    // Сбрасываем флаг доступности обновления
                    _scheduleUpdateAvailable.value = false
                    
                    
                    // UI автоматически обновится через StateFlow в RoutesViewModel
                    // Перезапуск НЕ НУЖЕН - данные обновляются реактивно ✅
                } else {
                    _scheduleRefreshError.value = "Не удалось загрузить данные с сервера"
                }
            } catch (e: Exception) {
                _scheduleRefreshError.value = e.message ?: "Неизвестная ошибка"
                Timber.e(e, "Ошибка обновления расписания с GitHub")
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
            Timber.e(e, "Ошибка проверки обновлений расписания")
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
                
                withContext(Dispatchers.IO) {
                    // Очищаем все DataStore
                    try {
                        context.dataStore.edit { it.clear() }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка очистки основного DataStore")
                    }
                    
                    try {
                        context.themeDataStore.edit { it.clear() }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка очистки DataStore темы")
                    }
                    
                    try {
                        context.displayDataStore.edit { it.clear() }
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка очистки DataStore отображения")
                    }
                    
                    // Удаляем файлы DataStore напрямую для полной очистки
                    try {
                        val dataStoreDir = File(context.filesDir, "datastore")
                        if (dataStoreDir.exists() && dataStoreDir.isDirectory) {
                            val files = dataStoreDir.listFiles()
                            files?.forEach { file ->
                                file.delete()
                            }
                        }
                    } catch (e: Exception) {
                    }
                    
                    // Очищаем SharedPreferences (если есть)
                    try {
                        val prefsDir = File(context.filesDir.parent, "shared_prefs")
                        if (prefsDir.exists() && prefsDir.isDirectory) {
                            val files = prefsDir.listFiles()
                            files?.forEach { file ->
                                file.delete()
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                
                
                // Даем время на завершение всех операций
                kotlinx.coroutines.delay(Constants.DATA_OPERATION_COMPLETION_DELAY_MS)
                
                // Перезапускаем приложение
                withContext(Dispatchers.Main) {
                    restartApp()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Критическая ошибка сброса настроек")
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
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка перезапуска приложения")
            // Принудительный выход
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

}