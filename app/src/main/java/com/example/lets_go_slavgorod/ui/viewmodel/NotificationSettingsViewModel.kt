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
                    Timber.w("Invalid notification mode in DataStore: $modeName, defaulting to ALL_DAYS")
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
                        Timber.w("Invalid day name in DataStore: $dayName")
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
     * Создается новый StateFlow при каждом вызове для обеспечения актуальных данных.
     * 
     * Используется в:
     * - RouteNotificationSettingsScreen для отображения текущего режима
     * - Для пересчета времени следующего уведомления
     * 
     * @param routeId идентификатор маршрута
     * @return StateFlow с режимом уведомлений для маршрута
     */
    fun getRouteNotificationMode(routeId: String): StateFlow<NotificationMode> {
        return getApplication<Application>().applicationContext.dataStore.data
            .map { preferences ->
                // Сначала пытаемся получить индивидуальный режим маршрута
                val routeModeName = preferences[stringPreferencesKey("$ROUTE_NOTIFICATION_MODE_PREFIX$routeId")]
                
                if (routeModeName != null) {
                    // Есть индивидуальный режим для маршрута
                    try {
                        val mode = NotificationMode.valueOf(routeModeName)
                        Timber.d("Route $routeId has custom mode: $mode")
                        mode
                    } catch (_: IllegalArgumentException) {
                        Timber.w("Invalid notification mode for route $routeId: $routeModeName")
                        NotificationMode.ALL_DAYS
                    }
                } else {
                    // Нет индивидуального режима - используем глобальный из preferences
                    val globalModeName = preferences[NOTIFICATION_MODE_KEY] ?: NotificationMode.ALL_DAYS.name
                    try {
                        val mode = NotificationMode.valueOf(globalModeName)
                        Timber.d("Route $routeId using global mode: $mode")
                        mode
                    } catch (_: IllegalArgumentException) {
                        Timber.w("Invalid global notification mode: $globalModeName")
                        NotificationMode.ALL_DAYS
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = NotificationMode.ALL_DAYS
            )
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
     * Создается новый StateFlow при каждом вызове для обеспечения актуальных данных.
     * 
     * @param routeId идентификатор маршрута
     * @return StateFlow с набором DayOfWeek для маршрута
     */
    fun getRouteSelectedDays(routeId: String): StateFlow<Set<DayOfWeek>> {
        return getApplication<Application>().applicationContext.dataStore.data
            .map { preferences ->
                // Сначала пытаемся получить индивидуальные дни маршрута
                val routeDayNames = preferences[stringSetPreferencesKey("$ROUTE_SELECTED_DAYS_PREFIX$routeId")]
                
                val dayNames = if (routeDayNames != null && routeDayNames.isNotEmpty()) {
                    // Есть индивидуальные дни для маршрута
                    Timber.d("Route $routeId has custom days: $routeDayNames")
                    routeDayNames
                } else {
                    // Нет индивидуальных дней - используем глобальные из preferences
                    val globalDays = preferences[SELECTED_DAYS_KEY] ?: emptySet()
                    Timber.d("Route $routeId using global days: $globalDays")
                    globalDays
                }
                
                dayNames.mapNotNull { dayName ->
                    try {
                        DayOfWeek.valueOf(dayName)
                    } catch (_: IllegalArgumentException) {
                        Timber.w("Invalid day name for route $routeId: $dayName")
                        null
                    }
                }.toSet()
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptySet()
            )
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
        viewModelScope.launch {
            try {
                Timber.d("═══════════════════════════════════════════════════")
                Timber.d("setRouteNotificationMode called:")
                Timber.d("  Route ID: $routeId")
                Timber.d("  New mode: $mode")
                
                val context = getApplication<Application>().applicationContext
                val key = stringPreferencesKey("$ROUTE_NOTIFICATION_MODE_PREFIX$routeId")
                
                Timber.d("  Saving with key: ${key.name}")
                
                // Сохраняем в DataStore
                context.dataStore.edit { preferences ->
                    preferences[key] = mode.name
                    Timber.d("  ✓ Written to DataStore: ${key.name} = ${mode.name}")
                    
                    // Удаляем дни если режим не SELECTED_DAYS
                    if (mode != NotificationMode.SELECTED_DAYS) {
                        val daysKey = stringSetPreferencesKey("$ROUTE_SELECTED_DAYS_PREFIX$routeId")
                        preferences.remove(daysKey)
                        Timber.d("  ✓ Removed selected days key")
                    }
                }
                
                // Проверяем что сохранилось (для диагностики)
                val verification = context.dataStore.data.first()
                val savedValue = verification[key]
                Timber.d("  VERIFICATION: Read back from DataStore: ${key.name} = $savedValue")
                
                if (savedValue == mode.name) {
                    Timber.d("  ✅ SUCCESS: Data verified in DataStore!")
                } else {
                    Timber.e("  ❌ ERROR: Data mismatch! Expected: ${mode.name}, Got: $savedValue")
                }
                
                // Обновляем кэш настроек и будильники
                NotificationPreferencesCache.updateCache(context)
                Timber.d("  ✓ Updated NotificationPreferencesCache")
                
                updateAllActiveAlarms()
                Timber.d("  ✓ Updated alarms")
                
                Timber.d("═══════════════════════════════════════════════════")
            } catch (e: Exception) {
                Timber.e(e, "❌ EXCEPTION in setRouteNotificationMode: ${e.message}")
                e.printStackTrace()
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
        viewModelScope.launch {
            try {
                Timber.d("═══════════════════════════════════════════════════")
                Timber.d("setRouteSelectedDays called:")
                Timber.d("  Route ID: $routeId")
                Timber.d("  Selected days: $days")
                
                val context = getApplication<Application>().applicationContext
                val dayNames = days.map { it.name }.toSet()
                val key = stringSetPreferencesKey("$ROUTE_SELECTED_DAYS_PREFIX$routeId")
                
                context.dataStore.edit { preferences ->
                    preferences[key] = dayNames
                    Timber.d("  ✓ Saved to DataStore: ${key.name} = $dayNames")
                }
                
                // Проверка сохранения (для диагностики)
                val verification = context.dataStore.data.first()
                val savedValue = verification[key]
                Timber.d("  VERIFICATION: $savedValue")
                
                // Обновляем кэш настроек и будильники
                NotificationPreferencesCache.updateCache(context)
                updateAllActiveAlarms()
                
                Timber.d("  ✅ Selected days saved successfully")
                Timber.d("═══════════════════════════════════════════════════")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in setRouteSelectedDays")
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
        viewModelScope.launch {
            try {
                Timber.d("═══════════════════════════════════════════════════")
                Timber.d("setGlobalNotificationMode called: $mode")
                
                val context = getApplication<Application>().applicationContext
                
                context.dataStore.edit { preferences ->
                    preferences[NOTIFICATION_MODE_KEY] = mode.name
                    Timber.d("  ✓ Saved global mode: ${NOTIFICATION_MODE_KEY.name} = ${mode.name}")
                    
                    if (mode != NotificationMode.SELECTED_DAYS) {
                        preferences.remove(SELECTED_DAYS_KEY)
                        Timber.d("  ✓ Removed global selected days")
                    }
                }
                
                // Проверка сохранения (для диагностики)
                val verification = context.dataStore.data.first()
                val savedValue = verification[NOTIFICATION_MODE_KEY]
                Timber.d("  VERIFICATION: $savedValue")
                
                NotificationPreferencesCache.updateCache(context)
                updateAllActiveAlarms()
                
                Timber.d("  ✅ Global mode saved successfully")
                Timber.d("═══════════════════════════════════════════════════")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in setGlobalNotificationMode")
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
        viewModelScope.launch {
            try {
                Timber.d("═══════════════════════════════════════════════════")
                Timber.d("setGlobalSelectedDays called: $days")
                
                val context = getApplication<Application>().applicationContext
                val dayNames = days.map { it.name }.toSet()
                
                context.dataStore.edit { preferences ->
                    preferences[SELECTED_DAYS_KEY] = dayNames
                    Timber.d("  ✓ Saved global days: ${SELECTED_DAYS_KEY.name} = $dayNames")
                }
                
                // Проверка сохранения (для диагностики)
                val verification = context.dataStore.data.first()
                val savedValue = verification[SELECTED_DAYS_KEY]
                Timber.d("  VERIFICATION: $savedValue")
                
                NotificationPreferencesCache.updateCache(context)
                updateAllActiveAlarms()
                
                Timber.d("  ✅ Global days saved successfully")
                Timber.d("═══════════════════════════════════════════════════")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error in setGlobalSelectedDays")
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
                val repository = BusRouteRepository(getApplication())
                
                val favoriteTimeEntities: List<FavoriteTimeEntity> = favoriteTimeDao.getAllFavoriteTimes().firstOrNull() ?: emptyList()
                
                val activeFavoriteTimes: List<FavoriteTime> = favoriteTimeEntities
                    .filter { entity: FavoriteTimeEntity -> entity.isActive }
                    .map { entity: FavoriteTimeEntity -> entity.toFavoriteTime(repository) }
                
                AlarmScheduler.updateAllAlarmsBasedOnSettings(getApplication(), activeFavoriteTimes)
                Timber.d("Updated ${activeFavoriteTimes.size} active alarms")
                
            } catch (e: Exception) {
                Timber.e(e, "Error updating active alarms")
            }
        }
    }
}