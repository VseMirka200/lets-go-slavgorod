/**
 * ViewModel для управления настройками обновлений приложения
 * 
 * Этот ViewModel отвечает за:
 * - Управление настройками автоматической проверки обновлений
 * - Выполнение ручной проверки обновлений
 * - Отображение статуса проверки обновлений
 * - Управление информацией о доступных обновлениях
 * 
 * @author VseMirka
 * @version 1.0
 * @since 2024
 */
package com.example.lets_go_slavgorod.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.data.local.UpdatePreferences
import timber.log.Timber
import com.example.lets_go_slavgorod.updates.UpdateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel для управления настройками обновлений
 * 
 * Предоставляет реактивные потоки данных для UI и методы
 * для управления настройками обновлений приложения.
 * 
 * @param context Контекст приложения для доступа к UpdatePreferences
 */
class UpdateSettingsViewModel(private val context: Context) : ViewModel() {
    
    // Экземпляр для работы с настройками обновлений
    private val updatePreferences = UpdatePreferences(context)
    
    // Потоки данных из UpdatePreferences
    /** Поток состояния автоматической проверки обновлений */
    val autoUpdateCheckEnabled: Flow<Boolean> = updatePreferences.autoUpdateCheckEnabled
    
    /** Поток времени последней проверки обновлений */
    val lastUpdateCheckTime: Flow<Long> = updatePreferences.lastUpdateCheckTime
    
    /** Поток версии доступного обновления */
    val availableUpdateVersion: Flow<String?> = updatePreferences.availableUpdateVersion
    
    /** Поток URL для скачивания обновления */
    val availableUpdateUrl: Flow<String?> = updatePreferences.availableUpdateUrl
    
    /** Поток описания изменений в обновлении */
    val availableUpdateNotes: Flow<String?> = updatePreferences.availableUpdateNotes

    // Преобразуем boolean в UpdateMode для удобства UI
    /** Текущий режим обновлений (автоматический/ручной) */
    val currentUpdateMode: Flow<UpdateMode> = autoUpdateCheckEnabled.map { enabled ->
        if (enabled) UpdateMode.AUTOMATIC else UpdateMode.MANUAL
    }

    // Состояние процесса проверки обновлений
    /** Внутреннее состояние процесса проверки обновлений */
    private val _isCheckingUpdates = MutableStateFlow(false)
    /** Публичный поток состояния проверки обновлений */
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    // Состояние ошибок
    /** Внутреннее состояние ошибки проверки обновлений */
    private val _updateCheckError = MutableStateFlow<String?>(null)
    /** Публичный поток ошибок проверки обновлений */
    val updateCheckError: StateFlow<String?> = _updateCheckError.asStateFlow()

    // Состояние статусных сообщений
    /** Внутреннее состояние статусного сообщения */
    private val _updateCheckStatus = MutableStateFlow<String?>(null)
    /** Публичный поток статусных сообщений */
    val updateCheckStatus: StateFlow<String?> = _updateCheckStatus.asStateFlow()

    /**
     * Устанавливает режим обновлений
     * 
     * @param mode Режим обновлений (AUTOMATIC, MANUAL, DISABLED)
     */
    fun setUpdateMode(mode: UpdateMode) {
        viewModelScope.launch {
            when (mode) {
                UpdateMode.AUTOMATIC -> updatePreferences.setAutoUpdateCheckEnabled(true)
                UpdateMode.MANUAL -> updatePreferences.setAutoUpdateCheckEnabled(false)
                UpdateMode.DISABLED -> updatePreferences.setAutoUpdateCheckEnabled(false)
            }
        }
    }
    
    /**
     * Выполняет ручную проверку обновлений
     * 
     * Запускает процесс проверки обновлений, обновляет состояние UI
     * и сохраняет результат в UpdatePreferences.
     */
    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            _updateCheckError.value = null
            _updateCheckStatus.value = null
            
            try {
                val updateManager = UpdateManager(context)
                val result = updateManager.checkForUpdatesWithResult()
                
                if (result.success) {
                    if (result.update != null) {
                        // Сохраняем информацию о доступном обновлении
                        updatePreferences.setAvailableUpdate(
                            version = result.update.versionName,
                            url = result.update.downloadUrl,
                            notes = result.update.releaseNotes
                        )
                        _updateCheckStatus.value = "Доступна новая версия ${result.update.versionName}"
                        Timber.d("Найдено обновление: ${result.update.versionName}")
                    } else {
                        // Очищаем информацию о доступном обновлении
                        updatePreferences.clearAvailableUpdate()
                        _updateCheckStatus.value = "У вас установлена последняя версия"
                        Timber.d("Обновления не найдены - у пользователя последняя версия")
                    }
                    
                    // Обновляем время последней проверки
                    updatePreferences.setLastUpdateCheckTime(System.currentTimeMillis())
                } else {
                    _updateCheckError.value = result.error ?: "Ошибка при проверке обновлений"
                    Timber.e("Ошибка при проверке обновлений: ${result.error}")
                }
            } catch (e: Exception) {
                _updateCheckError.value = "Ошибка: ${e.message}"
            } finally {
                _isCheckingUpdates.value = false
            }
        }
    }

    /**
     * Очищает статусное сообщение проверки обновлений
     */
    fun clearUpdateCheckStatus() {
        _updateCheckStatus.value = null
    }
    
    /**
     * Очищает информацию о доступном обновлении
     * 
     * Удаляет из UpdatePreferences информацию о доступном обновлении,
     * включая версию, URL и описание изменений.
     */
    fun clearAvailableUpdate() {
        viewModelScope.launch {
            updatePreferences.clearAvailableUpdate()
        }
    }
}
