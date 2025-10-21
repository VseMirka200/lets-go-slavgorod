package com.example.lets_go_slavgorod.utils

import com.example.lets_go_slavgorod.data.model.FavoriteTime
import com.example.lets_go_slavgorod.data.local.NotificationPreferencesCache
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
 * @version 1.0
 * @since 2.1
 */
object NotificationTimeCalculator {
    
    /**
     * Находит время следующего уведомления среди всех избранных
     * 
     * Правильно обрабатывает случаи когда:
     * - Несколько избранных времен в один день недели
     * - Текущее время между двумя избранными временами в один день
     * - Конвертация из Calendar формата дней (1=ВС) в Java Time (1=ПН)
     * 
     * @param favoriteTimes список избранных времён
     * @param leadTimeMinutes время уведомления заранее (в минутах)
     * @return LocalDateTime следующего уведомления или null
     */
    fun getNextNotificationTime(
        favoriteTimes: List<FavoriteTime>,
        leadTimeMinutes: Int = Constants.DEFAULT_NOTIFICATION_LEAD_TIME
    ): LocalDateTime? {
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value // 1=ПН, 7=ВС
        
        Timber.d("═══════════════════════════════════════════════════")
        Timber.d("Calculating next notification")
        Timber.d("Current time: $now")
        Timber.d("Current day of week: $currentDayOfWeek (${now.dayOfWeek})")
        Timber.d("Lead time: $leadTimeMinutes minutes")
        Timber.d("Active favorites: ${favoriteTimes.filter { it.isActive }.size}")
        Timber.d("───────────────────────────────────────────────────")
        
        val upcomingNotifications = favoriteTimes
            .filter { it.isActive }
            .mapNotNull { favoriteTime ->
                Timber.d("Processing favorite: ${favoriteTime.departureTime} on day ${favoriteTime.dayOfWeek}")
                val notificationTime = calculateNotificationTime(favoriteTime, leadTimeMinutes, now)
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
     * Алгоритм:
     * 1. Проверяет режим уведомлений (ALL_DAYS, WEEKDAYS, SELECTED_DAYS, DISABLED)
     * 2. Находит ближайшее появление нужного дня недели с учетом режима
     * 3. Создает дату+время отправления
     * 4. Вычитает leadTime для получения времени уведомления
     * 5. Если время уведомления в прошлом -> ищет следующий подходящий день
     * 
     * Пример: Сегодня ПН 12:00, избранное ПН 14:00, leadTime=15мин, режим=WEEKDAYS
     * - Ближайший будний = сегодня (ПН)
     * - Отправление = ПН 14:00
     * - Уведомление = ПН 13:45
     * - 13:45 > 12:00 -> оставляем сегодня ✓
     * 
     * Пример 2: Сегодня ПН 12:00, избранное ПН 10:00, leadTime=15мин, режим=WEEKDAYS
     * - Ближайший будний = сегодня (ПН)
     * - Отправление = ПН 10:00
     * - Уведомление = ПН 09:45
     * - 09:45 < 12:00 -> ищем следующий будний ПН ✓
     */
    private fun calculateNotificationTime(
        favoriteTime: FavoriteTime,
        leadTimeMinutes: Int,
        currentTime: LocalDateTime
    ): LocalDateTime? {
        return try {
            // Получаем режим уведомлений для маршрута
            val notificationMode = NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
            Timber.d("    Notification mode for route ${favoriteTime.routeId}: $notificationMode")
            
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
            
            // Проверяем, подходит ли этот день недели для режима уведомлений
            if (!isDayAllowedForMode(targetDayOfWeek, notificationMode, favoriteTime.routeId)) {
                Timber.d("    ✗ Day $targetDayOfWeek not allowed for mode $notificationMode")
                return null
            }
            
            // Находим ближайшую дату для этого дня недели с учетом режима (может быть сегодня)
            var targetDate = findNextOccurrenceWithMode(
                currentTime.toLocalDate(), 
                targetDayOfWeek, 
                notificationMode, 
                favoriteTime.routeId
            )
            Timber.d("    Step 3: Next occurrence of $targetDayOfWeek (mode: $notificationMode): $targetDate")
            
            // Объединяем дату и время
            var departureDateTime = LocalDateTime.of(targetDate, departureTime)
            Timber.d("    Step 4: Departure date+time: $departureDateTime")
            
            // Вычитаем время уведомления заранее
            var notificationDateTime = departureDateTime.minusMinutes(leadTimeMinutes.toLong())
            Timber.d("    Step 5: Notification time (departure - $leadTimeMinutes min): $notificationDateTime")
            
            // Если вычисленное время уведомления в прошлом или равно текущему, ищем следующий подходящий день
            // Это важно для корректной обработки нескольких избранных в один день
            if (notificationDateTime.isBefore(currentTime) || notificationDateTime.isEqual(currentTime)) {
                Timber.d("    Step 6: ⚠️ Notification time $notificationDateTime is in the past (current: $currentTime)")
                Timber.d("    Step 7: Finding next suitable day...")
                
                // Ищем следующий день, начиная со следующего дня
                targetDate = findNextOccurrenceWithMode(
                    targetDate.plusDays(1), 
                    targetDayOfWeek, 
                    notificationMode, 
                    favoriteTime.routeId
                )
                departureDateTime = LocalDateTime.of(targetDate, departureTime)
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
     */
    private fun isDayAllowedForMode(dayOfWeek: DayOfWeek, mode: NotificationMode, routeId: String): Boolean {
        return when (mode) {
            NotificationMode.ALL_DAYS -> true
            NotificationMode.WEEKDAYS -> dayOfWeek in listOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
            )
            NotificationMode.SELECTED_DAYS -> {
                val selectedDays = NotificationPreferencesCache.getSelectedDays(routeId)
                dayOfWeek in selectedDays
            }
            NotificationMode.DISABLED -> false
        }
    }
    
    /**
     * Находит следующее появление дня недели с учетом режима уведомлений
     */
    private fun findNextOccurrenceWithMode(
        startDate: LocalDate, 
        targetDay: DayOfWeek, 
        mode: NotificationMode,
        routeId: String
    ): LocalDate {
        var date = startDate
        
        // Ищем ближайший подходящий день (максимум 4 недели вперёд для безопасности)
        for (i in 0..27) {
            if (date.dayOfWeek == targetDay && isDayAllowedForMode(date.dayOfWeek, mode, routeId)) {
                return date
            }
            date = date.plusDays(1)
        }
        
        // Если не нашли за 4 недели - возвращаем исходную дату
        Timber.w("Could not find suitable date for $targetDay within 4 weeks")
        return startDate
    }
    
    /**
     * Находит следующее появление дня недели (без учета режима - для обратной совместимости)
     */
    private fun findNextOccurrence(startDate: LocalDate, targetDay: DayOfWeek): LocalDate {
        var date = startDate
        
        // Ищем ближайший день (максимум 7 дней вперёд)
        for (i in 0..6) {
            if (date.dayOfWeek == targetDay) {
                return date
            }
            date = date.plusDays(1)
        }
        
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
    
    /**
     * Форматирует время уведомления в читаемый вид
     */
    fun formatNotificationTime(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        
        return when {
            dateTime.toLocalDate() == now.toLocalDate() -> {
                "Сегодня в ${dateTime.format(formatter)}"
            }
            dateTime.toLocalDate() == now.toLocalDate().plusDays(1) -> {
                "Завтра в ${dateTime.format(formatter)}"
            }
            else -> {
                val dayFormatter = DateTimeFormatter.ofPattern("dd.MM в HH:mm")
                dateTime.format(dayFormatter)
            }
        }
    }
}

