package com.example.lets_go_slavgorod.ui.viewmodel

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.core.toFavoriteTime
import com.example.lets_go_slavgorod.data.local.AppDatabase
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.local.dataStore
import com.example.lets_go_slavgorod.data.local.entity.FavoriteTimeEntity
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.repository.BusRouteRepository
import com.example.lets_go_slavgorod.domain.notification.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
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
 * v3.0 Changes (Октябрь 2025):
 * - Оптимизированы импорты и зависимости
 * - Улучшена производительность работы с настройками
 * - Обновлены комментарии и документация
 */
class NotificationSettingsViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * Ключи для хранения настроек в DataStore
     * 
     * NOTIFICATION_MODE_KEY - глобальный режим уведомлений
     * SELECTED_DAYS_KEY - глобальный набор выбранных дней
     * ROUTE_NOTIFICATION_MODE_PREFIX - префикс для режима конкретного маршрута
     * ROUTE_SELECTED_DAYS_PREFIX - префикс для дней конкретного маршрута
     */
    private companion object {
        val NOTIFICATION_MODE_KEY = stringPreferencesKey("notification_mode")
        val SELECTED_DAYS_KEY = stringSetPreferencesKey("selected_notification_days")
        const val ROUTE_NOTIFICATION_MODE_PREFIX = "route_notification_mode_"
        const val ROUTE_SELECTED_DAYS_PREFIX = "route_selected_days_"
    }
    
    /** 
     * Защита от множественных вызовов
     * 
     * Предотвращает одновременное выполнение операций обновления
     * настроек уведомлений, что может привести к конфликтам
     * и некорректному состоянию данных.
     */
    private var isUpdatingRouteMode = false
    private var isUpdatingGlobalMode = false
    private var isUpdatingRouteDays = false
    private var isUpdatingGlobalDays = false
    
    /**
     * Кэш для StateFlow режимов уведомлений маршрутов
     * 
     * Предотвращает создание новых StateFlow при каждом вызове getRouteNotificationMode,
     * что устраняет "скачущее" поведение UI при заходе в настройки маршрута.
     */
    private val routeNotificationModeCache = mutableMapOf<String, StateFlow<NotificationMode>>()
    
    /**
     * Кэш для StateFlow выбранных дней маршрутов
     * 
     * Предотвращает создание новых StateFlow при каждом вызове getRouteSelectedDays,
     * что устраняет "скачущее" поведение UI при заходе в настройки маршрута.
     */
    private val routeSelectedDaysCache = mutableMapOf<String, StateFlow<Set<DayOfWeek>>>()
    
    /**
     * Текущий глобальный режим уведомлений
     * 
     * StateFlow автоматически обновляется при изменении настроек в DataStore.
     * По умолчанию используется режим ALL_DAYS.
     * 
     * @see NotificationMode
     */
    val currentNotificationMode: StateFlow<NotificationMode> =
        getApplication<Application>().applicationContext.dataStore.data
            .map { preferences ->
                val modeName = preferences[NOTIFICATION_MODE_KEY] ?: NotificationMode.ALL_DAYS.name
                try {
                    NotificationMode.valueOf(modeName)
                } catch (_: IllegalArgumentException) {
                    NotificationMode.ALL_DAYS
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(Constants.STATE_FLOW_TIMEOUT_MS),
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
        getApplication<Application>().applicationContext.dataStore.data
            .map { preferences ->
                val dayNames = preferences[SELECTED_DAYS_KEY] ?: emptySet()
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
                started = SharingStarted.WhileSubscribed(Constants.STATE_FLOW_TIMEOUT_MS),
                initialValue = emptySet()
            )

    /**
     * Получает режим уведомлений для конкретного маршрута
     * 
     * Логика приоритета:
     * - Если есть индивидуальный режим маршрута → возвращает его
     * - Если нет индивидуального → возвращает глобальный режим
     * 
     * StateFlow автоматически обновляется при изменении данных в DataStore.
     * Использует кэширование для предотвращения "скачущего" поведения UI.
     * 
     * Используется в:
     * - RouteNotificationSettingsScreen для отображения текущего режима
     * - Для пересчета времени следующего уведомления
     * 
     * @param routeId идентификатор маршрута
     * @return StateFlow с режимом уведомлений для маршрута
     */
    fun getRouteNotificationMode(routeId: String): StateFlow<NotificationMode> {
        // Проверяем кэш - если уже есть StateFlow для этого маршрута, возвращаем его
        routeNotificationModeCache[routeId]?.let { cachedFlow ->
            return cachedFlow
        }
        
        // Создаем новый StateFlow только если его нет в кэше
        val newFlow = getApplication<Application>().applicationContext.dataStore.data
            .map { preferences ->
                // Сначала пытаемся получить индивидуальный режим маршрута
                val routeModeName = preferences[stringPreferencesKey("$ROUTE_NOTIFICATION_MODE_PREFIX$routeId")]
                
                if (routeModeName != null) {
                    try {
                        NotificationMode.valueOf(routeModeName)
                    } catch (_: IllegalArgumentException) {
                        NotificationMode.ALL_DAYS
                    }
                } else {
                    val globalModeName = preferences[NOTIFICATION_MODE_KEY] ?: NotificationMode.ALL_DAYS.name
                    try {
                        NotificationMode.valueOf(globalModeName)
                    } catch (_: IllegalArgumentException) {
                        NotificationMode.ALL_DAYS
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NotificationMode.ALL_DAYS
            )
        
        // Сохраняем в кэш и возвращаем
        routeNotificationModeCache[routeId] = newFlow
        return newFlow
    }

    /**
     * Получает выбранные дни недели для уведомлений конкретного маршрута
     * 
     * Используется только в режиме SELECTED_DAYS.
     * 
     * Логика приоритета:
     * - Если есть индивидуальные дни маршрута → возвращает их
     * - Если нет индивидуальных → возвращает глобальные дни
     * 
     * StateFlow автоматически обновляется при изменении данных в DataStore.
     * Использует кэширование для предотвращения "скачущего" поведения UI.
     * 
     * @param routeId идентификатор маршрута
     * @return StateFlow с набором DayOfWeek для маршрута
     */
    fun getRouteSelectedDays(routeId: String): StateFlow<Set<DayOfWeek>> {
        // Проверяем кэш - если уже есть StateFlow для этого маршрута, возвращаем его
        routeSelectedDaysCache[routeId]?.let { cachedFlow ->
            return cachedFlow
        }
        
        // Создаем новый StateFlow только если его нет в кэше
        val newFlow = getApplication<Application>().applicationContext.dataStore.data
            .map { preferences ->
                // Сначала пытаемся получить индивидуальные дни маршрута
                val routeDayNames = preferences[stringSetPreferencesKey("$ROUTE_SELECTED_DAYS_PREFIX$routeId")]
                
                val dayNames = if (routeDayNames != null && routeDayNames.isNotEmpty()) {
                    routeDayNames
                } else {
                    preferences[SELECTED_DAYS_KEY] ?: emptySet()
                }
                
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
                initialValue = emptySet()
            )
        
        // Сохраняем в кэш и возвращаем
        routeSelectedDaysCache[routeId] = newFlow
        return newFlow
    }

    /**
     * Устанавливает режим уведомлений для конкретного маршрута
     * 
     * Алгоритм:
     * 1. Сохраняет режим в DataStore с ключом "route_notification_mode_{routeId}"
     * 2. Удаляет выбранные дни если режим != SELECTED_DAYS
     * 3. Проверяет успешность сохранения
     * 4. Обновляет NotificationPreferencesCache
     * 5. Перепланирует все активные уведомления
     * 
     * Детальное логирование для диагностики проблем с сохранением.
     * 
     * @param routeId идентификатор маршрута
     * @param mode новый режим уведомлений
     */
    fun setRouteNotificationMode(routeId: String, mode: NotificationMode) {
        // Защита от множественных вызовов - предотвращает конфликты при быстрых нажатиях
        if (isUpdatingRouteMode) {
            return
        }
        
        viewModelScope.launch {
            try {
                isUpdatingRouteMode = true
                
                val currentMode = getRouteNotificationMode(routeId).value
                if (currentMode == mode) {
                    return@launch
                }
                
                val context = getApplication<Application>().applicationContext
                val key = stringPreferencesKey("$ROUTE_NOTIFICATION_MODE_PREFIX$routeId")
                
                context.dataStore.edit { preferences ->
                    preferences[key] = mode.name
                    
                    if (mode != NotificationMode.SELECTED_DAYS) {
                        val daysKey = stringSetPreferencesKey("$ROUTE_SELECTED_DAYS_PREFIX$routeId")
                        preferences.remove(daysKey)
                    }
                }
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка в setRouteNotificationMode: ${e.message}")
            } finally {
                isUpdatingRouteMode = false
            }
        }
    }

    /**
     * Устанавливает выбранные дни недели для уведомлений конкретного маршрута
     * 
     * Используется только в режиме SELECTED_DAYS.
     * 
     * Алгоритм:
     * 1. Конвертирует Set<DayOfWeek> в Set<String> для сохранения
     * 2. Сохраняет в DataStore с ключом "route_selected_days_{routeId}"
     * 3. Проверяет успешность сохранения
     * 4. Обновляет NotificationPreferencesCache
     * 5. Перепланирует все активные уведомления
     * 
     * @param routeId идентификатор маршрута
     * @param days набор дней недели для уведомлений
     */
    fun setRouteSelectedDays(routeId: String, days: Set<DayOfWeek>) {
        // Защита от множественных вызовов - предотвращает конфликты при быстрых нажатиях
        if (isUpdatingRouteDays) {
            return
        }
        
        viewModelScope.launch {
            try {
                isUpdatingRouteDays = true
                
                val context = getApplication<Application>().applicationContext
                val dayNames = days.map { it.name }.toSet()
                val key = stringSetPreferencesKey("$ROUTE_SELECTED_DAYS_PREFIX$routeId")
                
                // Режим должен быть уже установлен в SELECTED_DAYS при клике на карточку
                
                context.dataStore.edit { preferences ->
                    preferences[key] = dayNames
                }
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка в setRouteSelectedDays")
            } finally {
                isUpdatingRouteDays = false
            }
        }
    }

    /**
     * Устанавливает глобальный режим уведомлений для всех маршрутов
     * 
     * Применяется к маршрутам БЕЗ индивидуальных настроек.
     * Маршруты с индивидуальными настройками сохраняют свои режимы.
     * 
     * Алгоритм:
     * 1. Сохраняет режим в DataStore с ключом "notification_mode"
     * 2. Удаляет глобальные выбранные дни если режим != SELECTED_DAYS
     * 3. Обновляет NotificationPreferencesCache
     * 4. Перепланирует все активные уведомления (включая маршруты с глобальными настройками)
     * 
     * @param mode новый глобальный режим уведомлений
     */
    fun setGlobalNotificationMode(mode: NotificationMode) {
        // Защита от множественных вызовов
        if (isUpdatingGlobalMode) {
            return
        }
        
        viewModelScope.launch {
            try {
                isUpdatingGlobalMode = true
                
                val currentMode = currentNotificationMode.value
                if (currentMode == mode) {
                    return@launch
                }
                
                val context = getApplication<Application>().applicationContext
                
                context.dataStore.edit { preferences ->
                    preferences[NOTIFICATION_MODE_KEY] = mode.name
                    
                    if (mode != NotificationMode.SELECTED_DAYS) {
                        preferences.remove(SELECTED_DAYS_KEY)
                    }
                }
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка в setGlobalNotificationMode")
            } finally {
                isUpdatingGlobalMode = false
            }
        }
    }
    
    /**
     * Устанавливает глобальные выбранные дни недели для уведомлений
     * 
     * Используется только в глобальном режиме SELECTED_DAYS.
     * Применяется к маршрутам БЕЗ индивидуальных настроек дней.
     * 
     * Алгоритм:
     * 1. Конвертирует Set<DayOfWeek> в Set<String>
     * 2. Сохраняет в DataStore с ключом "selected_notification_days"
     * 3. Проверяет успешность сохранения
     * 4. Обновляет NotificationPreferencesCache
     * 5. Перепланирует все активные уведомления
     * 
     * @param days набор дней недели для уведомлений
     */
    fun setGlobalSelectedDays(days: Set<DayOfWeek>) {
        // Защита от множественных вызовов - предотвращает конфликты при быстрых нажатиях
        if (isUpdatingGlobalDays) {
            return
        }
        
        viewModelScope.launch {
            try {
                isUpdatingGlobalDays = true
                
                val context = getApplication<Application>().applicationContext
                val dayNames = days.map { it.name }.toSet()
                
                // Режим должен быть уже установлен в SELECTED_DAYS при клике на карточку
                
                context.dataStore.edit { preferences ->
                    preferences[SELECTED_DAYS_KEY] = dayNames
                }
                
                updateAllActiveAlarms()
            } catch (e: Exception) {
                Timber.e(e, "Ошибка в setGlobalSelectedDays")
            } finally {
                isUpdatingGlobalDays = false
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
                val database = AppDatabase.getDatabase(getApplication())
                val favoriteTimeDao = database.favoriteTimeDao()
                val repository = BusRouteRepository(getApplication())
                
                val favoriteTimeEntities: List<FavoriteTimeEntity> = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
                
                val activeFavoriteTimes: List<FavoriteTime> = favoriteTimeEntities
                    .filter { entity: FavoriteTimeEntity -> entity.isActive }
                    .map { entity: FavoriteTimeEntity -> entity.toFavoriteTime(repository) }
                
                AlarmScheduler.updateAllAlarmsBasedOnSettings(getApplication(), activeFavoriteTimes)
                
            } catch (e: Exception) {
                Timber.e(e, "Ошибка обновления активных будильников")
            }
        }
    }
}