package com.example.lets_go_slavgorod.domain.notification

import android.content.Context
import com.example.lets_go_slavgorod.core.Constants
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.ui.viewmodel.NotificationMode
import timber.log.Timber
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Калькулятор времени следующего уведомления
 * 
 * Вычисляет точное время срабатывания ближайшего уведомления
 * на основе избранных времён и настроек уведомлений.
 * 
 * @author VseMirka200
 * @version 2.0
 * @since 2.1
 */
object NotificationTimeCalculator {
    
    /**
     * Находит время следующего уведомления среди всех избранных
     * 
     * ВАЖНО: Теперь получает leadTime для каждого маршрута индивидуально!
     * 
     * Правильно обрабатывает случаи когда:
     * - Несколько избранных времен в один день недели
     * - Текущее время между двумя избранными временами в один день
     * - Конвертация из Calendar формата дней (1=ВС) в Java Time (1=ПН)
     * - Разные маршруты имеют разные настройки времени уведомления
     * 
     * Оптимизация:
     * - Если все избранные принадлежат одному маршруту, использует переданный leadTime
     * - Если разные маршруты, читает индивидуальные настройки из DataStore
     * 
     * @param favoriteTimes список избранных времён
     * @param context контекст для доступа к настройкам (опционально)
     * @param leadTimeMinutes время уведомления заранее (в минутах) - используется для одного маршрута или как fallback
     * @param overrideNotificationMode режим уведомлений для одного маршрута (переопределяет кэш, для UI)
     * @param overrideSelectedDays выбранные дни для одного маршрута (переопределяет кэш, для UI)
     * @return LocalDateTime следующего уведомления или null
     */
    fun getNextNotificationTime(
        favoriteTimes: List<FavoriteTime>,
        context: Context? = null,
        leadTimeMinutes: Int = Constants.DEFAULT_NOTIFICATION_LEAD_TIME,
        overrideNotificationMode: NotificationMode? = null,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): LocalDateTime? {
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value // 1=ПН, 7=ВС
        
        Timber.d("═══════════════════════════════════════════════════")
        Timber.d("Calculating next notification")
        Timber.d("Current time: $now")
        Timber.d("Current day of week: $currentDayOfWeek (${now.dayOfWeek})")
        Timber.d("Context provided: ${context != null}")
        Timber.d("Provided lead time: $leadTimeMinutes minutes")
        Timber.d("Active favorites: ${favoriteTimes.filter { it.isActive }.size}")
        
        // Проверяем, все ли избранные принадлежат одному маршруту
        val activeFavorites = favoriteTimes.filter { it.isActive }
        val uniqueRouteIds = activeFavorites.map { it.routeId }.distinct()
        val isSingleRoute = uniqueRouteIds.size == 1
        
        Timber.d("Unique routes: ${uniqueRouteIds.size} ${if (isSingleRoute) "(using provided leadTime)" else "(reading from cache)"}")
        if (overrideNotificationMode != null) {
            Timber.d("Override mode: $overrideNotificationMode")
        }
        if (overrideSelectedDays != null) {
            Timber.d("Override selected days: $overrideSelectedDays")
        }
        Timber.d("───────────────────────────────────────────────────")
        
        val upcomingNotifications = activeFavorites
            .mapNotNull { favoriteTime ->
                Timber.d("Processing favorite: ${favoriteTime.departureTime} on day ${favoriteTime.dayOfWeek} for route ${favoriteTime.routeId}")
                
                // Получаем время уведомления для маршрута
                val actualLeadTime = if (isSingleRoute) {
                    // Оптимизация: если все избранные одного маршрута, используем переданный leadTime
                    // Это важно для RouteNotificationSettingsScreen где leadTime уже реактивный
                    Timber.d("  Using PROVIDED lead time: $leadTimeMinutes min (single route)")
                    leadTimeMinutes
                } else {
                    // Для разных маршрутов получаем настройки из кэша (синхронно, без блокировки)
                    val cached = NotificationPreferencesCache.getLeadTimeForRoute(favoriteTime.routeId)
                    Timber.d("  Using CACHED lead time: $cached min (multiple routes)")
                    cached
                }
                
                Timber.d("  ▶ ACTUAL lead time for route ${favoriteTime.routeId}: $actualLeadTime minutes")
                
                // Передаем override параметры только если это одномаршрутный список
                val notificationTime = if (isSingleRoute && (overrideNotificationMode != null || overrideSelectedDays != null)) {
                    calculateNotificationTime(
                        favoriteTime, 
                        actualLeadTime, 
                        now,
                        overrideNotificationMode,
                        overrideSelectedDays
                    )
                } else {
                    calculateNotificationTime(favoriteTime, actualLeadTime, now)
                }
                
                if (notificationTime != null) {
                    val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), notificationTime.toLocalDate())
                    Timber.d("  → Notification at: $notificationTime (in $daysDiff days)")
                } else {
                    Timber.d("  → Could not calculate notification time")
                }
                notificationTime
            }
            .filter { 
                val isAfter = it.isAfter(now)
                if (!isAfter) {
                    Timber.d("Filtering out past notification: $it")
                }
                isAfter
            }
            .sorted()
        
        val nextNotification = upcomingNotifications.firstOrNull()
        Timber.d("───────────────────────────────────────────────────")
        Timber.d("RESULT: Next notification at $nextNotification")
        Timber.d("═══════════════════════════════════════════════════")
        
        return nextNotification
    }
    
    /**
     * Вычисляет время уведомления для конкретного избранного времени
     * 
     * Алгоритм зависит от режима уведомлений:
     * 
     * SELECTED_DAYS (Выбранные дни):
     * - Уведомления только в выбранные дни недели
     * - Ищет следующее появление дня недели избранного времени среди выбранных дней
     * 
     * ALL_DAYS (Все дни) / WEEKDAYS (Только будни):
     * - Уведомления каждый день / каждый будний день в указанное время
     * - Игнорирует день недели избранного времени
     * - Ищет ближайший подходящий день (сегодня или завтра)
     * 
     * Пример 1: Избранное ПН 10:00, режим=ALL_DAYS, сейчас ПН 14:00
     * - Ближайший разрешённый день = завтра (ВТ)
     * - Отправление = ВТ 10:00
     * - Уведомление = ВТ 09:45 (leadTime=15мин)
     * 
     * Пример 2: Избранное ПН 10:00, режим=SELECTED_DAYS [ПН,СР], сейчас ПН 14:00
     * - Ближайший разрешённый день = следующая СР
     * - Отправление = СР 10:00
     * - Уведомление = СР 09:45
     * 
     * @param overrideNotificationMode если задан, используется вместо значения из кэша (для UI)
     * @param overrideSelectedDays если заданы, используются вместо значений из кэша (для UI)
     */
    private fun calculateNotificationTime(
        favoriteTime: FavoriteTime,
        leadTimeMinutes: Int,
        currentTime: LocalDateTime,
        overrideNotificationMode: NotificationMode? = null,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): LocalDateTime? {
        return try {
            // Получаем режим уведомлений для маршрута (используем override если есть)
            val notificationMode = overrideNotificationMode 
                ?: NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
            Timber.d("    Notification mode for route ${favoriteTime.routeId}: $notificationMode${if (overrideNotificationMode != null) " (overridden)" else ""}")
            
            // Если уведомления отключены для этого маршрута - пропускаем
            if (notificationMode == NotificationMode.DISABLED) {
                Timber.d("    ✗ Notifications DISABLED for route ${favoriteTime.routeId}")
                return null
            }
            
            // Парсим время отправления
            val departureTime = LocalTime.parse(favoriteTime.departureTime, DateTimeFormatter.ofPattern("HH:mm"))
            Timber.d("    Step 1: Departure time parsed: $departureTime")
            
            // Конвертируем день недели из Calendar формата (1=ВС, 2=ПН) в Java Time формат (1=ПН, 7=ВС)
            val targetDayOfWeek = convertCalendarDayToJavaTime(favoriteTime.dayOfWeek)
            Timber.d("    Step 2: Target day of week: Calendar format ${favoriteTime.dayOfWeek} -> Java Time $targetDayOfWeek")
            
            // НОВАЯ ЛОГИКА: в зависимости от режима уведомлений
            val targetDate: LocalDate
            
            when (notificationMode) {
                NotificationMode.SELECTED_DAYS -> {
                    // Режим "Выбранные дни": уведомления только в выбранные дни недели
                    // Игнорируем день недели избранного времени и ищем ближайший выбранный день
                    // Обеспечивает согласованное поведение со всеми режимами
                    targetDate = findNextOccurrenceAnyDay(
                        currentTime.toLocalDate(),
                        notificationMode,
                        favoriteTime.routeId,
                        overrideSelectedDays
                    )
                    Timber.d("    Step 3 (SELECTED_DAYS): Next selected day: $targetDate")
                }
                
                NotificationMode.ALL_DAYS, NotificationMode.WEEKDAYS -> {
                    // Режим "Все дни" или "Только будни": игнорируем день недели избранного
                    // Уведомления будут каждый день/будний день в указанное время
                    targetDate = findNextOccurrenceAnyDay(
                        currentTime.toLocalDate(),
                        notificationMode,
                        favoriteTime.routeId,
                        overrideSelectedDays
                    )
                    Timber.d("    Step 3 ($notificationMode): Next allowed day: $targetDate")
                }
                
                NotificationMode.DISABLED -> {
                    return null
                }
            }
            
            // Объединяем дату и время
            var departureDateTime = LocalDateTime.of(targetDate, departureTime)
            Timber.d("    Step 4: Departure date+time: $departureDateTime")
            
            // Вычитаем время уведомления заранее
            var notificationDateTime = departureDateTime.minusMinutes(leadTimeMinutes.toLong())
            Timber.d("    Step 5: Notification time (departure - $leadTimeMinutes min): $notificationDateTime")
            
            // Если вычисленное время уведомления в прошлом или равно текущему, ищем следующий подходящий день
            if (notificationDateTime.isBefore(currentTime) || notificationDateTime.isEqual(currentTime)) {
                Timber.d("    Step 6: ⚠️ Notification time $notificationDateTime is in the past (current: $currentTime)")
                Timber.d("    Step 7: Finding next suitable day...")
                
                // Ищем следующий день - теперь все режимы работают одинаково
                val nextDate = when (notificationMode) {
                    NotificationMode.SELECTED_DAYS, NotificationMode.ALL_DAYS, NotificationMode.WEEKDAYS -> {
                        findNextOccurrenceAnyDay(
                            targetDate.plusDays(1),
                            notificationMode,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                    NotificationMode.DISABLED -> return null
                }
                
                departureDateTime = LocalDateTime.of(nextDate, departureTime)
                notificationDateTime = departureDateTime.minusMinutes(leadTimeMinutes.toLong())
                Timber.d("    Step 8: New notification time: $notificationDateTime")
            } else {
                Timber.d("    Step 6: ✓ Notification time is in the future")
            }
            
            notificationDateTime
            
        } catch (e: Exception) {
            Timber.e(e, "Error calculating notification time for ${favoriteTime.id}")
            null
        }
    }
    
    /**
     * Проверяет, подходит ли день недели для режима уведомлений
     * 
     * @param dayOfWeek день недели для проверки (Java Time формат: 1=ПН, 7=ВС)
     * @param mode режим уведомлений маршрута
     * @param routeId ID маршрута (для получения списка выбранных дней в режиме SELECTED_DAYS)
     * @param overrideSelectedDays если заданы, используются вместо значений из кэша (для UI)
     * @return true если день подходит для отправки уведомлений
     */
    private fun isDayAllowedForMode(
        dayOfWeek: DayOfWeek, 
        mode: NotificationMode, 
        routeId: String,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): Boolean {
        return when (mode) {
            NotificationMode.ALL_DAYS -> true
            NotificationMode.WEEKDAYS -> dayOfWeek in listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
            NotificationMode.SELECTED_DAYS -> {
                val selectedDays = overrideSelectedDays 
                    ?: NotificationPreferencesCache.getSelectedDays(routeId)
                dayOfWeek in selectedDays
            }
            NotificationMode.DISABLED -> false
        }
    }
    
    /**
     * Находит ближайший разрешённый день для режимов ALL_DAYS или WEEKDAYS
     * 
     * В отличие от findNextOccurrenceWithMode, эта функция не ищет конкретный день недели,
     * а находит ЛЮБОЙ ближайший разрешённый день (сегодня, завтра и т.д.)
     * 
     * @param startDate дата с которой начинать поиск
     * @param mode режим уведомлений (ALL_DAYS или WEEKDAYS)
     * @param routeId ID маршрута для получения настроек
     * @param overrideSelectedDays не используется для ALL_DAYS/WEEKDAYS
     * @return дата следующего подходящего дня
     */
    private fun findNextOccurrenceAnyDay(
        startDate: LocalDate,
        mode: NotificationMode,
        routeId: String,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): LocalDate {
        var date = startDate
        
        // Ищем ближайший подходящий день (максимум 14 дней вперёд для безопасности)
        for (i in 0..13) {
            if (isDayAllowedForMode(date.dayOfWeek, mode, routeId, overrideSelectedDays)) {
                return date
            }
            date = date.plusDays(1)
        }
        
        // Если не нашли за 2 недели - возвращаем исходную дату (edge case, не должно случиться для ALL_DAYS/WEEKDAYS)
        Timber.w("Could not find suitable date for mode $mode within 2 weeks")
        return startDate
    }
    
    /**
     * Находит следующее появление конкретного дня недели с учетом режима уведомлений
     * 
     * Ищет ближайшую дату когда:
     * 1. День недели совпадает с targetDay
     * 2. День разрешен согласно режиму уведомлений (SELECTED_DAYS)
     * 
     * @param startDate дата с которой начинать поиск
     * @param targetDay целевой день недели
     * @param mode режим уведомлений
     * @param routeId ID маршрута для получения настроек
     * @param overrideSelectedDays если заданы, используются вместо значений из кэша (для UI)
     * @return дата следующего подходящего дня (максимум в пределах 4 недель)
     */
    private fun findNextOccurrenceWithMode(
        startDate: LocalDate, 
        targetDay: DayOfWeek, 
        mode: NotificationMode,
        routeId: String,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): LocalDate {
        var date = startDate
        
        // Ищем ближайший подходящий день (максимум 4 недели вперёд для безопасности)
        for (i in 0..27) {
            if (date.dayOfWeek == targetDay && isDayAllowedForMode(date.dayOfWeek, mode, routeId, overrideSelectedDays)) {
                return date
            }
            date = date.plusDays(1)
        }
        
        // Если не нашли за 4 недели - возвращаем исходную дату (edge case)
        Timber.w("Could not find suitable date for $targetDay within 4 weeks")
        return startDate
    }
    
    /**
     * Конвертирует день недели из формата Calendar в формат Java Time
     * 
     * Calendar формат (из BusSchedule):
     * - 1 = Воскресенье
     * - 2 = Понедельник
     * - 3 = Вторник
     * - 4 = Среда
     * - 5 = Четверг
     * - 6 = Пятница
     * - 7 = Суббота
     * 
     * Java Time формат (DayOfWeek):
     * - 1 = Понедельник
     * - 2 = Вторник
     * - 3 = Среда
     * - 4 = Четверг
     * - 5 = Пятница
     * - 6 = Суббота
     * - 7 = Воскресенье
     * 
     * @param calendarDay день недели в формате Calendar (1-7)
     * @return DayOfWeek в формате Java Time
     */
    private fun convertCalendarDayToJavaTime(calendarDay: Int): DayOfWeek {
        return when (calendarDay) {
            1 -> DayOfWeek.SUNDAY      // Calendar: 1 = ВС -> Java Time: 7 = ВС
            2 -> DayOfWeek.MONDAY      // Calendar: 2 = ПН -> Java Time: 1 = ПН
            3 -> DayOfWeek.TUESDAY     // Calendar: 3 = ВТ -> Java Time: 2 = ВТ
            4 -> DayOfWeek.WEDNESDAY   // Calendar: 4 = СР -> Java Time: 3 = СР
            5 -> DayOfWeek.THURSDAY    // Calendar: 5 = ЧТ -> Java Time: 4 = ЧТ
            6 -> DayOfWeek.FRIDAY      // Calendar: 6 = ПТ -> Java Time: 5 = ПТ
            7 -> DayOfWeek.SATURDAY    // Calendar: 7 = СБ -> Java Time: 6 = СБ
            else -> {
                Timber.e("Invalid calendar day: $calendarDay, defaulting to Monday")
                DayOfWeek.MONDAY
            }
        }
    }
}