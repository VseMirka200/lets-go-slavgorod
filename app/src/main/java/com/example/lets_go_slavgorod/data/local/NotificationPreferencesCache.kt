package com.example.lets_go_slavgorod.data.local

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import com.example.lets_go_slavgorod.ui.viewmodel.QuietMode
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.DayOfWeek
import java.util.concurrent.ConcurrentHashMap

/**
 * Кэш настроек уведомлений для синхронного доступа из BroadcastReceiver
 * 
 * Решает проблему невозможности использования suspend функций в BroadcastReceiver.
 * Предоставляет in-memory кэш настроек уведомлений который обновляется асинхронно
 * из DataStore, но может быть прочитан синхронно из любого места.
 * 
 * Архитектура:
 * - Все поля помечены @Volatile для потокобезопасности
 * - Кэш обновляется при старте приложения и после каждого изменения настроек
 * - Синхронные getters для чтения без блокировки потока
 * - Suspend setter для обновления из ViewModels
 * 
 * Кэшируемые настройки:
 * - Тихий режим (глобальное отключение уведомлений)
 * - Режим уведомлений (все дни/будни/выбранные дни/отключено)
 * - Выбранные дни недели для уведомлений
 * - Настройки вибрации
 * - Индивидуальные настройки для каждого маршрута
 * 
 * Использование:
 * ```kotlin
 * // Обновление кэша (из ViewModel или Application)
 * NotificationPreferencesCache.updateCache(context)
 * 
 * // Синхронное чтение (из AlarmReceiver)
 * val shouldSend = NotificationPreferencesCache.shouldSendNotification(routeId)
 * ```
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 2.0
 * 
 * @see com.example.lets_go_slavgorod.data.notification.AlarmReceiver
 * @see QuietMode
 * @see NotificationMode
 */
object NotificationPreferencesCache {
    
    /** Текущий режим тихого режима (глобальное отключение) */
    @Volatile
    private var quietMode: QuietMode = QuietMode.ENABLED
    
    /** Timestamp окончания тихого режима для CUSTOM_DAYS */
    @Volatile
    private var quietUntilTime: Long? = null
    
    /** Глобальный режим уведомлений */
    @Volatile
    private var notificationMode: NotificationMode = NotificationMode.ALL_DAYS
    
    /** Глобальный набор выбранных дней недели */
    @Volatile
    private var selectedDays: Set<DayOfWeek> = emptySet()
    
    /** Индивидуальные режимы уведомлений для каждого маршрута (thread-safe) */
    private val routeNotificationModes: ConcurrentHashMap<String, NotificationMode> = ConcurrentHashMap()
    
    /** Индивидуальные наборы дней для каждого маршрута (thread-safe) */
    private val routeSelectedDays: ConcurrentHashMap<String, Set<DayOfWeek>> = ConcurrentHashMap()
    
    /** Флаг включения вибрации при получении уведомлений */
    @Volatile
    private var vibrationEnabled: Boolean = true
    
    /** Глобальное время опережения уведомления (в минутах) */
    @Volatile
    private var globalLeadTime: Int = Constants.DEFAULT_NOTIFICATION_LEAD_TIME
    
    /** Индивидуальное время опережения для каждого маршрута (в минутах, thread-safe) */
    private val routeLeadTimes: ConcurrentHashMap<String, Int> = ConcurrentHashMap()
    
    /**
     * Асинхронное обновление всего кэша из DataStore
     * 
     * Загружает все настройки из DataStore и обновляет кэш.
     * Должно вызываться:
     * - При старте приложения (BusApplication.onCreate)
     * - После каждого изменения настроек в ViewModels
     * 
     * Все ошибки обрабатываются gracefully с использованием значений по умолчанию.
     * 
     * @param context контекст для доступа к DataStore
     */
    suspend fun updateCache(context: Context) {
        try {
            val preferences = context.dataStore.data.first()
            
            // Обновляем тихий режим
            val quietModeString = preferences[stringPreferencesKey("quiet_mode")]
            quietMode = try {
                QuietMode.valueOf(quietModeString ?: QuietMode.ENABLED.name)
            } catch (e: IllegalArgumentException) {
                QuietMode.ENABLED
            }
            
            quietUntilTime = preferences[androidx.datastore.preferences.core.longPreferencesKey("quiet_until")]
            
            // Обновляем режим уведомлений
            val notifModeString = preferences[stringPreferencesKey("notification_mode")]
            notificationMode = try {
                NotificationMode.valueOf(notifModeString ?: NotificationMode.ALL_DAYS.name)
            } catch (e: IllegalArgumentException) {
                NotificationMode.ALL_DAYS
            }
            
            // Обновляем выбранные дни
            val daysSet = preferences[stringSetPreferencesKey("selected_notification_days")] ?: emptySet()
            selectedDays = daysSet.mapNotNull { dayName ->
                try {
                    DayOfWeek.valueOf(dayName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toSet()
            
            // Обновляем настройку вибрации
            vibrationEnabled = preferences[androidx.datastore.preferences.core.booleanPreferencesKey("vibration_enabled")] ?: true
            
            // Обновляем глобальное время опережения
            globalLeadTime = preferences[androidx.datastore.preferences.core.intPreferencesKey("global_notification_lead_time")] 
                ?: Constants.DEFAULT_NOTIFICATION_LEAD_TIME
            
            // Загружаем индивидуальные настройки для каждого маршрута
            val tempRouteModes = ConcurrentHashMap<String, NotificationMode>()
            val tempRouteDays = ConcurrentHashMap<String, Set<DayOfWeek>>()
            val tempRouteLeadTimes = ConcurrentHashMap<String, Int>()
            
            // Проходим по всем ключам в preferences и ищем настройки маршрутов
            preferences.asMap().forEach { (key, value) ->
                val keyName = key.name
                
                // Загружаем режим уведомлений для конкретного маршрута
                if (keyName.startsWith("route_notification_mode_")) {
                    val routeId = keyName.removePrefix("route_notification_mode_")
                    val modeString = value as? String
                    if (modeString != null) {
                        try {
                            tempRouteModes[routeId] = NotificationMode.valueOf(modeString)
                            Timber.d("Loaded route mode: $routeId -> $modeString")
                        } catch (e: IllegalArgumentException) {
                            Timber.w("Invalid NotificationMode for route $routeId: $modeString")
                        }
                    }
                }
                
                // Загружаем выбранные дни для конкретного маршрута
                if (keyName.startsWith("route_selected_days_")) {
                    val routeId = keyName.removePrefix("route_selected_days_")
                    @Suppress("UNCHECKED_CAST")
                    val dayNamesSet = value as? Set<String>
                    if (dayNamesSet != null) {
                        val days = dayNamesSet.mapNotNull { dayName ->
                            try {
                                DayOfWeek.valueOf(dayName)
                            } catch (e: IllegalArgumentException) {
                                Timber.w("Invalid DayOfWeek for route $routeId: $dayName")
                                null
                            }
                        }.toSet()
                        tempRouteDays[routeId] = days
                        Timber.d("Loaded route days: $routeId -> $days")
                    }
                }
                
                // Загружаем индивидуальное время опережения для конкретного маршрута
                if (keyName.startsWith("route_") && keyName.endsWith("_lead_time")) {
                    val routeId = keyName.removePrefix("route_").removeSuffix("_lead_time")
                    val leadTime = value as? Int
                    if (leadTime != null && leadTime > 0) {
                        tempRouteLeadTimes[routeId] = leadTime
                        Timber.d("Loaded route lead time: $routeId -> $leadTime minutes")
                    }
                }
            }
            
            // Обновляем мапы (ConcurrentHashMap уже thread-safe)
            routeNotificationModes.clear()
            routeNotificationModes.putAll(tempRouteModes)
            
            routeSelectedDays.clear()
            routeSelectedDays.putAll(tempRouteDays)
            
            routeLeadTimes.clear()
            routeLeadTimes.putAll(tempRouteLeadTimes)
            
            Timber.d("NotificationPreferencesCache updated: quietMode=$quietMode, notificationMode=$notificationMode, vibration=$vibrationEnabled, globalLeadTime=$globalLeadTime")
            Timber.d("Route-specific settings: ${routeNotificationModes.size} modes, ${routeSelectedDays.size} day sets, ${routeLeadTimes.size} lead times")
        } catch (e: Exception) {
            Timber.e(e, "Error updating notification preferences cache")
        }
    }
    
    /**
     * Получает текущий режим тихого режима из кэша (синхронно)
     * 
     * Может вызываться из любого потока без блокировки.
     * 
     * @return текущий QuietMode
     */
    fun getQuietMode(): QuietMode = quietMode
    
    /**
     * Получает timestamp окончания тихого режима
     * 
     * Используется для проверки истечения таймера в режиме CUSTOM_DAYS.
     * 
     * @return timestamp окончания или null если не установлен
     */
    fun getQuietUntilTime(): Long? = quietUntilTime
    
    /**
     * Получает режим уведомлений для конкретного маршрута или глобальный
     * 
     * Если для маршрута установлен индивидуальный режим, возвращает его,
     * иначе возвращает глобальный режим.
     * 
     * @param routeId ID маршрута для получения индивидуальных настроек (опционально)
     * @return режим уведомлений для маршрута или глобальный
     */
    fun getNotificationMode(routeId: String? = null): NotificationMode {
        val mode = if (routeId != null && routeNotificationModes.containsKey(routeId)) {
            val customMode = routeNotificationModes[routeId] ?: notificationMode
            Timber.d("getNotificationMode($routeId): using CUSTOM mode = $customMode")
            customMode
        } else {
            Timber.d("getNotificationMode($routeId): using GLOBAL mode = $notificationMode")
            notificationMode
        }
        return mode
    }
    
    /**
     * Получает набор выбранных дней недели для уведомлений
     * 
     * Если для маршрута установлены индивидуальные дни, возвращает их,
     * иначе возвращает глобальный набор дней.
     * 
     * @param routeId ID маршрута для получения индивидуальных настроек (опционально)
     * @return набор дней недели когда должны отправляться уведомления
     */
    fun getSelectedDays(routeId: String? = null): Set<DayOfWeek> {
        val days = if (routeId != null && routeSelectedDays.containsKey(routeId)) {
            val customDays = routeSelectedDays[routeId] ?: selectedDays
            Timber.d("getSelectedDays($routeId): using CUSTOM days = $customDays")
            customDays
        } else {
            Timber.d("getSelectedDays($routeId): using GLOBAL days = $selectedDays")
            selectedDays
        }
        return days
    }
    
    /**
     * Проверяет, включена ли вибрация для уведомлений
     * 
     * @return true если вибрация должна срабатывать при уведомлениях
     */
    fun isVibrationEnabled(): Boolean = vibrationEnabled
    
    /**
     * Получает время опережения уведомления для конкретного маршрута или глобальное
     * 
     * Если для маршрута установлено индивидуальное время, возвращает его,
     * иначе возвращает глобальное время опережения.
     * 
     * Этот метод СИНХРОННЫЙ и НЕ блокирует поток - читает из in-memory кэша.
     * Кэш обновляется асинхронно через updateCache().
     * 
     * @param routeId ID маршрута для получения индивидуальных настроек (опционально)
     * @return время опережения в минутах
     */
    fun getLeadTimeForRoute(routeId: String? = null): Int {
        val leadTime = if (routeId != null && routeLeadTimes.containsKey(routeId)) {
            val customLeadTime = routeLeadTimes[routeId] ?: globalLeadTime
            Timber.d("getLeadTimeForRoute($routeId): using CUSTOM lead time = $customLeadTime minutes")
            customLeadTime
        } else {
            Timber.d("getLeadTimeForRoute($routeId): using GLOBAL lead time = $globalLeadTime minutes")
            globalLeadTime
        }
        return leadTime
    }
    
    /**
     * Комплексная проверка, должно ли быть отправлено уведомление (синхронно)
     * 
     * Проверяет все условия для отправки уведомления:
     * 1. Тихий режим (не должен быть DISABLED или активный CUSTOM_DAYS)
     * 2. Режим уведомлений маршрута (не DISABLED)
     * 3. Соответствие текущего дня выбранным дням (для SELECTED_DAYS и WEEKDAYS)
     * 
     * Эта функция вызывается из AlarmReceiver для принятия решения о показе уведомления.
     * 
     * @param routeId ID маршрута для проверки индивидуальных настроек (опционально)
     * @return true если уведомление должно быть отправлено, false если нет
     */
    fun shouldSendNotification(routeId: String? = null): Boolean {
        return try {
            Timber.d("Checking notification settings:")
            Timber.d("  RouteId: ${routeId ?: "global"}")
            
            // Проверяем тихий режим
            Timber.d("  QuietMode: $quietMode")
            when (quietMode) {
                QuietMode.DISABLED -> {
                    Timber.d("  ✗ Result: Quiet mode is DISABLED - no notifications")
                    return false
                }
                QuietMode.ENABLED -> {
                    Timber.d("  ✓ Quiet mode: ENABLED (notifications allowed)")
                }
                QuietMode.CUSTOM_DAYS -> {
                    if (quietUntilTime != null && System.currentTimeMillis() < quietUntilTime!!) {
                        val remainingTime = (quietUntilTime!! - System.currentTimeMillis()) / 1000 / 60
                        Timber.d("  ✗ Result: Quiet until timer active ($remainingTime min remaining)")
                        return false
                    } else {
                        Timber.d("  ✓ Quiet timer expired or not set")
                    }
                }
            }
            
            // Проверяем режим уведомлений
            val mode = getNotificationMode(routeId)
            Timber.d("  NotificationMode: $mode")
            
            val result = when (mode) {
                NotificationMode.DISABLED -> {
                    Timber.d("  ✗ Result: Notification mode is DISABLED")
                    false
                }
                NotificationMode.ALL_DAYS -> {
                    Timber.d("  ✓ Result: ALL_DAYS mode - sending notification")
                    true
                }
                NotificationMode.WEEKDAYS -> {
                    val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                    val isWeekday = currentDay in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
                    val dayName = when (currentDay) {
                        java.util.Calendar.SUNDAY -> "Sunday"
                        java.util.Calendar.MONDAY -> "Monday"
                        java.util.Calendar.TUESDAY -> "Tuesday"
                        java.util.Calendar.WEDNESDAY -> "Wednesday"
                        java.util.Calendar.THURSDAY -> "Thursday"
                        java.util.Calendar.FRIDAY -> "Friday"
                        java.util.Calendar.SATURDAY -> "Saturday"
                        else -> "Unknown"
                    }
                    Timber.d("  Current day: $dayName (code: $currentDay)")
                    if (isWeekday) {
                        Timber.d("  ✓ Result: WEEKDAYS mode - today is a weekday")
                    } else {
                        Timber.d("  ✗ Result: WEEKDAYS mode - today is weekend")
                    }
                    isWeekday
                }
                NotificationMode.SELECTED_DAYS -> {
                    val days = getSelectedDays(routeId)
                    val calendar = java.util.Calendar.getInstance()
                    val currentDay = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                    val currentDayOfWeek = when (currentDay) {
                        java.util.Calendar.SUNDAY -> DayOfWeek.SUNDAY
                        java.util.Calendar.MONDAY -> DayOfWeek.MONDAY
                        java.util.Calendar.TUESDAY -> DayOfWeek.TUESDAY
                        java.util.Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
                        java.util.Calendar.THURSDAY -> DayOfWeek.THURSDAY
                        java.util.Calendar.FRIDAY -> DayOfWeek.FRIDAY
                        java.util.Calendar.SATURDAY -> DayOfWeek.SATURDAY
                        else -> null
                    }
                    val shouldSend = currentDayOfWeek != null && currentDayOfWeek in days
                    Timber.d("  Current day: $currentDayOfWeek")
                    Timber.d("  Selected days: $days")
                    if (shouldSend) {
                        Timber.d("  ✓ Result: SELECTED_DAYS mode - today is in selected days")
                    } else {
                        Timber.d("  ✗ Result: SELECTED_DAYS mode - today is NOT in selected days")
                    }
                    shouldSend
                }
            }
            
            result
        } catch (e: Exception) {
            Timber.e(e, "✗ Error checking notification settings - defaulting to SEND")
            true // По умолчанию разрешаем уведомления
        }
    }
}