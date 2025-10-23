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
                        } catch (e: IllegalArgumentException) {
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
                                null
                            }
                        }.toSet()
                        tempRouteDays[routeId] = days
                    }
                }
                
                // Загружаем индивидуальное время опережения для конкретного маршрута
                if (keyName.startsWith("route_") && keyName.endsWith("_lead_time")) {
                    val routeId = keyName.removePrefix("route_").removeSuffix("_lead_time")
                    val leadTime = value as? Int
                    if (leadTime != null && leadTime > 0) {
                        tempRouteLeadTimes[routeId] = leadTime
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
            
        } catch (e: Exception) {
            Timber.e(e, "Ошибка обновления кэша настроек уведомлений")
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
            routeNotificationModes[routeId] ?: notificationMode
        } else {
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
            routeSelectedDays[routeId] ?: selectedDays
        } else {
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
            routeLeadTimes[routeId] ?: globalLeadTime
        } else {
            globalLeadTime
        }
        return leadTime
    }
    
    /**
     * Комплексная проверка, должно ли быть отправлено уведомление (синхронно)
     * 
     * Проверяет все условия для отправки уведомления:
     * 1. Тихий режим (НЕ блокирует уведомления - они приходят даже в тихом режиме)
     * 2. Режим уведомлений маршрута (не должен быть DISABLED)
     * 3. Соответствие текущего дня выбранным дням (для SELECTED_DAYS и WEEKDAYS)
     * 
     * ВАЖНО: Тихий режим больше НЕ блокирует уведомления!
     * Уведомления приходят даже когда включен тихий режим.
     * 
     * Эта функция вызывается из AlarmReceiver для принятия решения о показе уведомления.
     * 
     * @param routeId ID маршрута для проверки индивидуальных настроек (опционально)
     * @return true если уведомление должно быть отправлено, false если нет
     */
    fun shouldSendNotification(routeId: String? = null): Boolean {
        return try {
            val mode = getNotificationMode(routeId)
            
            when (mode) {
                NotificationMode.DISABLED -> false
                NotificationMode.ALL_DAYS -> true
                NotificationMode.WEEKDAYS -> {
                    val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                    currentDay in java.util.Calendar.MONDAY..java.util.Calendar.FRIDAY
                }
                NotificationMode.SELECTED_DAYS -> {
                    val days = getSelectedDays(routeId)
                    val currentDay = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
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
                    currentDayOfWeek != null && currentDayOfWeek in days
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Ошибка проверки настроек уведомлений")
            true
        }
    }
}