package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import timber.log.Timber
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.notifications.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.DayOfWeek

/**
 * Режимы уведомлений о времени отправления автобусов
 * 
 * Определяет различные стратегии для планирования уведомлений пользователей
 * в зависимости от их предпочтений и расписания.
 * 
 * - WEEKDAYS: уведомления только в будни (понедельник-пятница)
 * - ALL_DAYS: уведомления каждый день
 * - SELECTED_DAYS: уведомления в выбранные пользователем дни недели
 * - DISABLED: уведомления отключены
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
enum class NotificationMode {
    WEEKDAYS,
    ALL_DAYS,
    SELECTED_DAYS,
    DISABLED
}

/**
 * ViewModel для управления настройками уведомлений о времени отправления автобусов
 * 
 * Предоставляет централизованное управление всеми настройками уведомлений:
 * - Глобальные настройки для всех маршрутов
 * - Индивидуальные настройки для каждого маршрута
 * - Синхронизация с системой уведомлений через AlarmScheduler
 * 
 * Основные функции:
 * - Управление режимами уведомлений (все дни/будни/выбранные дни/отключено)
 * - Сохранение выбранных дней недели для уведомлений
 * - Обновление всех активных уведомлений при изменении настроек
 * - Интеграция с AlarmScheduler для планирования уведомлений
 * - Персистентное хранение через DataStore
 * 
 * Паттерны использования:
 * - Глобальные настройки применяются ко всем новым избранным временам
 * - Настройки для конкретного маршрута перекрывают глобальные
 * - При изменении настроек автоматически обновляются все активные будильники
 * 
 * @param application контекст приложения для доступа к DataStore и базе данных
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 1.0
 */
class NotificationSettingsViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Ключи для хранения настроек в DataStore
     * 
     * NOTIFICATION_MODE_KEY - глобальный режим уведомлений
     * SELECTED_DAYS_KEY - глобальный набор выбранных дней
     * ROUTE_NOTIFICATION_MODE_KEY - префикс для режима конкретного маршрута
     * ROUTE_SELECTED_DAYS_KEY - префикс для дней конкретного маршрута
     */
    private companion object {
        val NOTIFICATION_MODE_KEY = stringPreferencesKey("notification_mode")
        val SELECTED_DAYS_KEY = stringSetPreferencesKey("selected_notification_days")
        val ROUTE_NOTIFICATION_MODE_KEY = stringPreferencesKey("route_notification_mode_")
        val ROUTE_SELECTED_DAYS_KEY = stringSetPreferencesKey("route_selected_days_")
    }

    /**
     * Текущий глобальный режим уведомлений
     * 
     * StateFlow автоматически обновляется при изменении настроек в DataStore.
     * По умолчанию используется режим ALL_DAYS.
     * 
     * @see NotificationMode
     */
    val currentNotificationMode: StateFlow<NotificationMode> =
        getApplication<Application>().dataStore.data
            .map { preferences ->
                val modeName = preferences[NOTIFICATION_MODE_KEY] ?: NotificationMode.ALL_DAYS.name
                try {
                    NotificationMode.valueOf(modeName)
                } catch (_: IllegalArgumentException) {
                    Timber.w("Invalid notification mode in DataStore: $modeName, defaulting to ALL_DAYS")
                    NotificationMode.ALL_DAYS
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NotificationMode.ALL_DAYS
            )

    /**
     * Глобально выбранные дни недели для уведомлений
     * 
     * Используется когда режим уведомлений установлен в SELECTED_DAYS.
     * StateFlow автоматически обновляется при изменении настроек.
     * Некорректные значения дней фильтруются и логируются.
     * 
     * @return Set из DayOfWeek, пустой если дни не выбраны
     */
    val selectedNotificationDays: StateFlow<Set<DayOfWeek>> =
        getApplication<Application>().dataStore.data
            .map { preferences ->
                val dayNames = preferences[SELECTED_DAYS_KEY] ?: emptySet()
                dayNames.mapNotNull { dayName ->
                    try {
                        DayOfWeek.valueOf(dayName)
                    } catch (_: IllegalArgumentException) {
                        Timber.w("Invalid day name in DataStore: $dayName")
                        null
                    }
                }.toSet()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )

    /**
     * Получает режим уведомлений для конкретного маршрута
     * 
     * Если для маршрута не установлен собственный режим, используется глобальный.
     * Это позволяет пользователям настраивать уведомления индивидуально для каждого маршрута.
     * 
     * @param routeId идентификатор маршрута
     * @return StateFlow с режимом уведомлений для маршрута
     */
    fun getRouteNotificationMode(routeId: String): StateFlow<NotificationMode> =
        getApplication<Application>().dataStore.data
            .map { preferences ->
                val modeName = preferences[stringPreferencesKey("${ROUTE_NOTIFICATION_MODE_KEY.name}$routeId")]
                    ?: currentNotificationMode.value.name
                try {
                    NotificationMode.valueOf(modeName)
                } catch (_: IllegalArgumentException) {
                    currentNotificationMode.value
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = currentNotificationMode.value
            )

    /**
     * Получает выбранные дни недели для уведомлений конкретного маршрута
     * 
     * Если для маршрута не установлены собственные дни, используются глобальные.
     * Используется только когда режим уведомлений для маршрута установлен в SELECTED_DAYS.
     * 
     * @param routeId идентификатор маршрута
     * @return StateFlow с набором DayOfWeek для маршрута
     */
    fun getRouteSelectedDays(routeId: String): StateFlow<Set<DayOfWeek>> =
        getApplication<Application>().dataStore.data
            .map { preferences ->
                val dayNames = preferences[stringSetPreferencesKey("${ROUTE_SELECTED_DAYS_KEY.name}$routeId")]
                    ?: selectedNotificationDays.value.map { it.name }.toSet()
                dayNames.mapNotNull { dayName ->
                    try {
                        DayOfWeek.valueOf(dayName)
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }.toSet()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = selectedNotificationDays.value
            )

    /**
     * Устанавливает режим уведомлений для конкретного маршрута
     * 
     * Сохраняет настройку в DataStore и автоматически обновляет все активные будильники
     * для маршрута в соответствии с новым режимом. Если режим не SELECTED_DAYS,
     * очищает сохраненные дни для маршрута.
     * 
     * @param routeId идентификатор маршрута
     * @param mode новый режим уведомлений
     */
    fun setRouteNotificationMode(routeId: String, mode: NotificationMode) {
        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.edit { settings ->
                    settings[stringPreferencesKey("${ROUTE_NOTIFICATION_MODE_KEY.name}$routeId")] = mode.name
                    if (mode != NotificationMode.SELECTED_DAYS) {
                        settings.remove(stringSetPreferencesKey("${ROUTE_SELECTED_DAYS_KEY.name}$routeId"))
                    }
                }
                Timber.d("Route $routeId notification mode set to: ${mode.name}")
                
                // ВАЖНО: Обновляем кэш перед обновлением будильников
                NotificationPreferencesCache.updateCache(getApplication())
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save route notification mode")
            }
        }
    }

    /**
     * Устанавливает выбранные дни недели для уведомлений конкретного маршрута
     * 
     * Сохраняет дни в DataStore и автоматически обновляет все активные будильники.
     * Используется когда режим уведомлений для маршрута установлен в SELECTED_DAYS.
     * 
     * @param routeId идентификатор маршрута
     * @param days набор дней недели для уведомлений
     */
    fun setRouteSelectedDays(routeId: String, days: Set<DayOfWeek>) {
        viewModelScope.launch {
            try {
                val dayNames = days.map { it.name }.toSet()
                getApplication<Application>().dataStore.edit { settings ->
                    settings[stringSetPreferencesKey("${ROUTE_SELECTED_DAYS_KEY.name}$routeId")] = dayNames
                }
                Timber.d("Route $routeId selected days saved: $dayNames")
                
                // ВАЖНО: Обновляем кэш перед обновлением будильников
                NotificationPreferencesCache.updateCache(getApplication())
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save route selected days")
            }
        }
    }

    /**
     * Устанавливает глобальный режим уведомлений для всех маршрутов
     * 
     * Применяется ко всем маршрутам, у которых не установлен индивидуальный режим.
     * Сохраняет настройку в DataStore и обновляет все активные будильники.
     * Если режим не SELECTED_DAYS, очищает глобальные выбранные дни.
     * 
     * @param mode новый глобальный режим уведомлений
     */
    fun setGlobalNotificationMode(mode: NotificationMode) {
        viewModelScope.launch {
            try {
                getApplication<Application>().dataStore.edit { settings ->
                    settings[NOTIFICATION_MODE_KEY] = mode.name
                    if (mode != NotificationMode.SELECTED_DAYS) {
                        settings.remove(SELECTED_DAYS_KEY)
                    }
                }
                Timber.d("Global notification mode set to: ${mode.name}")
                
                // ВАЖНО: Обновляем кэш перед обновлением будильников
                NotificationPreferencesCache.updateCache(getApplication())
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save global notification mode")
            }
        }
    }
    
    /**
     * Устанавливает глобальные выбранные дни недели для уведомлений
     * 
     * Применяется ко всем маршрутам, у которых не установлены индивидуальные дни.
     * Используется когда глобальный режим установлен в SELECTED_DAYS.
     * Автоматически обновляет все активные будильники.
     * 
     * @param days набор дней недели для уведомлений
     */
    fun setGlobalSelectedDays(days: Set<DayOfWeek>) {
        viewModelScope.launch {
            try {
                val dayNames = days.map { it.name }.toSet()
                getApplication<Application>().dataStore.edit { settings ->
                    settings[SELECTED_DAYS_KEY] = dayNames
                }
                Timber.d("Global selected days saved: $dayNames")
                
                // ВАЖНО: Обновляем кэш перед обновлением будильников
                NotificationPreferencesCache.updateCache(getApplication())
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save global selected days")
            }
        }
    }
    
    /**
     * Обновляет все активные будильники для уведомлений
     * 
     * Вызывается автоматически при изменении любых настроек уведомлений.
     * Загружает все активные избранные времена из базы данных и передает их
     * в AlarmScheduler для пересоздания будильников с новыми настройками.
     * 
     * Процесс:
     * 1. Загружает все активные избранные времена из Room
     * 2. Преобразует entities в model objects
     * 3. Передает в AlarmScheduler для обновления
     * 
     * Выполняется асинхронно в viewModelScope.
     */
    private fun updateAllActiveAlarms() {
        viewModelScope.launch {
            try {
                Timber.d("Updating all active alarms based on notification settings")
                
                val database = AppDatabase.getDatabase(getApplication())
                val favoriteTimeDao = database.favoriteTimeDao()
                
                val favoriteTimeEntities = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
                
                val activeFavoriteTimes = favoriteTimeEntities
                    .filter { it.isActive }
                    .map { entity: FavoriteTimeEntity ->
                        FavoriteTime(
                            id = entity.id,
                            routeId = entity.routeId,
                            routeNumber = "N/A",
                            routeName = "Маршрут",
                            stopName = entity.stopName,
                            departureTime = entity.departureTime,
                            dayOfWeek = entity.dayOfWeek,
                            departurePoint = entity.departurePoint,
                            addedDate = entity.addedDate,
                            isActive = entity.isActive
                        )
                    }
                
                AlarmScheduler.updateAllAlarmsBasedOnSettings(getApplication(), activeFavoriteTimes)
                Timber.d("Updated ${activeFavoriteTimes.size} active alarms")
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating active alarms")
            }
        }
    }

}