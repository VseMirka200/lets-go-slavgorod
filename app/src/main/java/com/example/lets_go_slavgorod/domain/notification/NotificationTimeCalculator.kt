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
 * Калькулятор времени уведомлений
 * 
 * Центральный компонент для расчета времени уведомлений с учетом
 * избранных времен, режимов уведомлений и настроек пользователя.
 * 
 * Архитектурные особенности:
 * - Thread-safe singleton object
 * - Поддержка override параметров для UI
 * - Кэширование настроек через NotificationPreferencesCache
 * - Обработка различных режимов уведомлений
 * 
 * Основные функции:
 * - getNextNotificationTime: расчет времени уведомления
 * - getNextDepartureTime: расчет времени отправления
 * - Поддержка всех режимов уведомлений
 * 
 * @author VseMirka200
 * @version 2.1
 * @since 1.0
 */
object NotificationTimeCalculator {
    
    /**
     * Вычисляет время следующего отправления автобуса
     * 
     * Анализирует все активные избранные времена и определяет ближайшее
     * время отправления с учетом настроек уведомлений для каждого маршрута.
     * 
     * Алгоритм:
     * 1. Фильтрация активных избранных времен
     * 2. Расчет времени отправления для каждого избранного времени
     * 3. Фильтрация будущих времен
     * 4. Выбор ближайшего времени
     * 
     * @param favoriteTimes список избранных времен
     * @param context контекст приложения (опционально)
     * @param overrideNotificationMode переопределение режима уведомлений
     * @param overrideSelectedDays переопределение выбранных дней
     * @return время следующего отправления или null
     */
    fun getNextDepartureTime(
        favoriteTimes: List<FavoriteTime>,
        context: Context? = null,
        overrideNotificationMode: NotificationMode? = null,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): LocalDateTime? {
        // Фиксация времени для предотвращения race conditions
        val now = LocalDateTime.now()
        
        // Оптимизация: проверка на одномаршрутность для упрощения логики
        val activeFavorites = favoriteTimes.filter { it.isActive }
        val uniqueRouteIds = activeFavorites.map { it.routeId }.distinct()
        val isSingleRoute = uniqueRouteIds.size == 1
        
        Timber.d("NotificationTimeCalculator: activeFavorites count = ${activeFavorites.size}")
        Timber.d("NotificationTimeCalculator: isSingleRoute = $isSingleRoute")
        Timber.d("NotificationTimeCalculator: overrideNotificationMode = $overrideNotificationMode")
        Timber.d("NotificationTimeCalculator: overrideSelectedDays = $overrideSelectedDays")
        
        val upcomingDepartures = activeFavorites
            .mapNotNull { favoriteTime ->
                // Передаем override параметры только если это одномаршрутный список
                val departureTime = if (isSingleRoute && (overrideNotificationMode != null || overrideSelectedDays != null)) {
                    calculateDepartureTime(
                        favoriteTime, 
                        now, // Используем зафиксированное время
                        overrideNotificationMode,
                        overrideSelectedDays
                    )
                } else {
                    calculateDepartureTime(favoriteTime, now) // Используем зафиксированное время
                }
                
                departureTime
            }
            .filter { 
                // ИСПРАВЛЕНО: Используем зафиксированное время для консистентности
                val isAfter = it.isAfter(now)
                isAfter
            }
            .sorted()
        
        val nextDeparture = upcomingDepartures.firstOrNull()
        
        Timber.d("NotificationTimeCalculator: upcomingDepartures count = ${upcomingDepartures.size}")
        Timber.d("NotificationTimeCalculator: nextDeparture = $nextDeparture")
        
        return nextDeparture
    }

    /**
     * Вычисляет время следующего уведомления
     * 
     * Основная функция для расчета времени уведомления с учетом времени опережения.
     * Анализирует все активные избранные времена и определяет ближайшее время
     * срабатывания уведомления.
     * 
     * Алгоритм:
     * 1. Фильтрация активных избранных времен
     * 2. Получение настроек времени опережения для каждого маршрута
     * 3. Расчет времени уведомления для каждого избранного времени
     * 4. Фильтрация будущих времен
     * 5. Выбор ближайшего времени уведомления
     * 
     * @param favoriteTimes список избранных времен
     * @param context контекст приложения (опционально)
     * @param leadTimeMinutes время опережения в минутах
     * @param overrideNotificationMode переопределение режима уведомлений
     * @param overrideSelectedDays переопределение выбранных дней
     * @return время следующего уведомления или null
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
        
        
        // Проверяем, все ли избранные принадлежат одному маршруту
        val activeFavorites = favoriteTimes.filter { it.isActive }
        val uniqueRouteIds = activeFavorites.map { it.routeId }.distinct()
        val isSingleRoute = uniqueRouteIds.size == 1
        
        
        val upcomingNotifications = activeFavorites
            .mapNotNull { favoriteTime ->
                
                // Получаем время уведомления для маршрута
                val actualLeadTime = if (isSingleRoute) {
                    // Оптимизация: если все избранные одного маршрута, используем переданный leadTime
                    // Это важно для RouteNotificationSettingsScreen где leadTime уже реактивный
                    leadTimeMinutes
                } else {
                    val cached = NotificationPreferencesCache.getLeadTimeForRoute(favoriteTime.routeId)
                    cached
                }
                
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
                
                notificationTime
            }
            .filter { 
                val isAfter = it.isAfter(now)
                isAfter
            }
            .sorted()
        
        val nextNotification = upcomingNotifications.firstOrNull()
        
        return nextNotification
    }
    
    /**
     * Вычисляет время уведомления для одного избранного времени
     * 
     * Смотрит на избранное время и настройки маршрута,
     * чтобы понять, когда должно сработать уведомление.
     * 
     * Учитывает:
     * - Режим уведомлений (все дни, только будни, выбранные дни)
     * - День недели избранного времени
     * - Время опережения (за сколько минут уведомлять)
     * 
     * Возвращает точное время срабатывания уведомления.
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
            if (notificationMode == NotificationMode.DISABLED) {
                return null
            }
            
            // Парсим время отправления
            val departureTime = LocalTime.parse(favoriteTime.departureTime, DateTimeFormatter.ofPattern("HH:mm"))
            
            // Конвертируем день недели из Calendar формата (1=ВС, 2=ПН) в Java Time формат (1=ПН, 7=ВС)
            // ВАЖНО: Если dayOfWeek равен 0 или не установлен, то это означает "каждый день"
            val targetDayOfWeek = if (favoriteTime.dayOfWeek > 0) {
                convertCalendarDayToJavaTime(favoriteTime.dayOfWeek)
            } else {
                null // null означает "каждый день"
            }
            
            // НОВАЯ ЛОГИКА: в зависимости от режима уведомлений
            val targetDate: LocalDate
            
            when (notificationMode) {
                NotificationMode.SELECTED_DAYS -> {
                    // Режим "Выбранные дни": уведомления только в выбранные дни недели
                    val selectedDays = overrideSelectedDays ?: NotificationPreferencesCache.getSelectedDays(favoriteTime.routeId)
                    
                    if (targetDayOfWeek == null) {
                        // День недели не установлен - ищем любой выбранный день
                        targetDate = findNextOccurrenceAnyDay(
                            currentTime.toLocalDate(),
                            notificationMode,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    } else {
                        // День недели установлен - проверяем, что он входит в выбранные дни
                        if (targetDayOfWeek !in selectedDays) {
                            return null
                        }
                        
                        // Ищем следующий день, который соответствует дню недели избранного времени
                        targetDate = findNextOccurrenceForSelectedDays(
                            currentTime.toLocalDate(),
                            targetDayOfWeek,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                }
                
                NotificationMode.ALL_DAYS -> {
                    // Режим "Все дни": уведомления каждый день в указанное время
                    if (targetDayOfWeek == null) {
                        // День недели не установлен - планируем на сегодня или завтра
                        // ИСПРАВЛЕНО: учитываем, что автобус уже мог уйти сегодня
                        val todayDeparture = LocalDateTime.of(currentTime.toLocalDate(), departureTime)
                        targetDate = if (todayDeparture.isAfter(currentTime)) {
                            // Автобус еще не ушел сегодня
                            currentTime.toLocalDate()
                        } else {
                            // Автобус уже ушел сегодня, планируем на завтра
                            currentTime.toLocalDate().plusDays(1)
                        }
                    } else {
                        // День недели установлен - ищем следующий день, который соответствует дню недели избранного времени
                        targetDate = findNextOccurrenceForSelectedDays(
                            currentTime.toLocalDate(),
                            targetDayOfWeek,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                }
                
                NotificationMode.WEEKDAYS -> {
                    // Режим "Только будни": уведомления только в будние дни
                    if (targetDayOfWeek == null) {
                        // День недели не установлен - ищем следующий будний день
                        targetDate = findNextOccurrenceAnyDay(
                            currentTime.toLocalDate(),
                            notificationMode,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    } else {
                        // День недели установлен - проверяем, что он будний
                        if (targetDayOfWeek !in listOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        )) {
                            return null
                        }
                        
                        // Ищем следующий день, который соответствует дню недели избранного времени
                        targetDate = findNextOccurrenceForSelectedDays(
                            currentTime.toLocalDate(),
                            targetDayOfWeek,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                }
                
                NotificationMode.DISABLED -> {
                    return null
                }
            }
            
            // Объединяем дату и время
            var departureDateTime = LocalDateTime.of(targetDate, departureTime)
            var notificationDateTime = departureDateTime.minusMinutes(leadTimeMinutes.toLong())
            
            // Если вычисленное время уведомления в прошлом или равно текущему, ищем следующий подходящий день
            if (notificationDateTime.isBefore(currentTime) || notificationDateTime.isEqual(currentTime)) {
                
                // Ищем следующий день - теперь все режимы работают одинаково
                val nextDate = when (notificationMode) {
                    NotificationMode.SELECTED_DAYS, NotificationMode.WEEKDAYS -> {
                        findNextOccurrenceAnyDay(
                            targetDate.plusDays(1),
                            notificationMode,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                    NotificationMode.ALL_DAYS -> {
                        if (targetDayOfWeek == null) {
                            targetDate.plusDays(1)
                        } else {
                            findNextOccurrenceForDayOfWeek(
                                targetDate.plusDays(1),
                                targetDayOfWeek
                            )
                        }
                    }
                    NotificationMode.DISABLED -> return null
                }
                
                departureDateTime = LocalDateTime.of(nextDate, departureTime)
                notificationDateTime = departureDateTime.minusMinutes(leadTimeMinutes.toLong())
            }
            
            notificationDateTime
            
        } catch (e: Exception) {
            Timber.e(e, "Ошибка вычисления времени уведомления для ${favoriteTime.id}")
            null
        }
    }
    
    /**
     * Вычисляет время отправления автобуса для одного избранного времени
     * 
     * Смотрит на избранное время и настройки маршрута,
     * чтобы понять, когда должен отправиться автобус.
     * 
     * Учитывает:
     * - Режим уведомлений (все дни, только будни, выбранные дни)
     * - День недели избранного времени
     * 
     * Возвращает точное время отправления автобуса.
     */
    private fun calculateDepartureTime(
        favoriteTime: FavoriteTime,
        currentTime: LocalDateTime,
        overrideNotificationMode: NotificationMode? = null,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): LocalDateTime? {
        return try {
            Timber.d("calculateDepartureTime: favoriteTime = $favoriteTime")
            Timber.d("calculateDepartureTime: currentTime = $currentTime")
            Timber.d("calculateDepartureTime: overrideNotificationMode = $overrideNotificationMode")
            Timber.d("calculateDepartureTime: overrideSelectedDays = $overrideSelectedDays")
            
            // Получаем режим уведомлений для маршрута (используем override если есть)
            val notificationMode = overrideNotificationMode 
                ?: NotificationPreferencesCache.getNotificationMode(favoriteTime.routeId)
            
            Timber.d("calculateDepartureTime: notificationMode = $notificationMode")
            
            if (notificationMode == NotificationMode.DISABLED) {
                Timber.d("calculateDepartureTime: notifications disabled, returning null")
                return null
            }
            
            // Парсим время отправления
            val departureTime = LocalTime.parse(favoriteTime.departureTime, DateTimeFormatter.ofPattern("HH:mm"))
            
            // Конвертируем день недели из Calendar формата (1=ВС, 2=ПН) в Java Time формат (1=ПН, 7=ВС)
            // ВАЖНО: Если dayOfWeek равен 0 или не установлен, то это означает "каждый день"
            val targetDayOfWeek = if (favoriteTime.dayOfWeek > 0) {
                convertCalendarDayToJavaTime(favoriteTime.dayOfWeek)
            } else {
                null // null означает "каждый день"
            }
            
            // НОВАЯ ЛОГИКА: в зависимости от режима уведомлений
            val targetDate: LocalDate
            
            when (notificationMode) {
                NotificationMode.SELECTED_DAYS -> {
                    // Режим "Выбранные дни": уведомления только в выбранные дни недели
                    val selectedDays = overrideSelectedDays ?: NotificationPreferencesCache.getSelectedDays(favoriteTime.routeId)
                    
                    if (targetDayOfWeek == null) {
                        // День недели не установлен - ищем любой выбранный день
                        targetDate = findNextOccurrenceAnyDay(
                            currentTime.toLocalDate(),
                            notificationMode,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    } else {
                        // День недели установлен - проверяем, что он входит в выбранные дни
                        if (targetDayOfWeek !in selectedDays) {
                            return null
                        }
                        
                        // Ищем следующий день, который соответствует дню недели избранного времени
                        targetDate = findNextOccurrenceForSelectedDays(
                            currentTime.toLocalDate(),
                            targetDayOfWeek,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                }
                
                NotificationMode.ALL_DAYS -> {
                    // Режим "Все дни": уведомления каждый день в указанное время
                    if (targetDayOfWeek == null) {
                        // День недели не установлен - планируем на сегодня или завтра
                        // ИСПРАВЛЕНО: учитываем, что автобус уже мог уйти сегодня
                        val todayDeparture = LocalDateTime.of(currentTime.toLocalDate(), departureTime)
                        targetDate = if (todayDeparture.isAfter(currentTime)) {
                            // Автобус еще не ушел сегодня
                            currentTime.toLocalDate()
                        } else {
                            // Автобус уже ушел сегодня, планируем на завтра
                            currentTime.toLocalDate().plusDays(1)
                        }
                    } else {
                        // День недели установлен - ищем следующий день, который соответствует дню недели избранного времени
                        targetDate = findNextOccurrenceForSelectedDays(
                            currentTime.toLocalDate(),
                            targetDayOfWeek,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                }
                
                NotificationMode.WEEKDAYS -> {
                    // Режим "Только будни": уведомления только в будние дни
                    if (targetDayOfWeek == null) {
                        // День недели не установлен - ищем следующий будний день
                        targetDate = findNextOccurrenceAnyDay(
                            currentTime.toLocalDate(),
                            notificationMode,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    } else {
                        // День недели установлен - проверяем, что он будний
                        if (targetDayOfWeek !in listOf(
                            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, 
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY
                        )) {
                            return null
                        }
                        
                        // Ищем следующий день, который соответствует дню недели избранного времени
                        targetDate = findNextOccurrenceForSelectedDays(
                            currentTime.toLocalDate(),
                            targetDayOfWeek,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                }
                
                NotificationMode.DISABLED -> {
                    return null
                }
            }
            
            // Объединяем дату и время
            var departureDateTime = LocalDateTime.of(targetDate, departureTime)
            
            // Если вычисленное время отправления в прошлом или равно текущему, ищем следующий подходящий день
            if (departureDateTime.isBefore(currentTime) || departureDateTime.isEqual(currentTime)) {
                
                // Ищем следующий день - теперь все режимы работают одинаково
                val nextDate = when (notificationMode) {
                    NotificationMode.SELECTED_DAYS, NotificationMode.WEEKDAYS -> {
                        findNextOccurrenceAnyDay(
                            targetDate.plusDays(1),
                            notificationMode,
                            favoriteTime.routeId,
                            overrideSelectedDays
                        )
                    }
                    NotificationMode.ALL_DAYS -> {
                        if (targetDayOfWeek == null) {
                            targetDate.plusDays(1)
                        } else {
                            findNextOccurrenceForDayOfWeek(
                                targetDate.plusDays(1),
                                targetDayOfWeek
                            )
                        }
                    }
                    NotificationMode.DISABLED -> return null
                }
                
                departureDateTime = LocalDateTime.of(nextDate, departureTime)
            }
            
            departureDateTime
            
        } catch (e: Exception) {
            Timber.e(e, "Ошибка вычисления времени отправления для ${favoriteTime.id}")
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
     * Находит следующий подходящий день для уведомлений
     * 
     * В зависимости от режима уведомлений ищет:
     * - ALL_DAYS: любой день (сегодня или завтра)
     * - WEEKDAYS: следующий будний день
     * - SELECTED_DAYS: следующий выбранный день
     * 
     * Пример: режим "Только будни", сегодня суббота → возвращает понедельник
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
            // ИСПРАВЛЕНО: Сначала проверяем, что день недели совпадает с целевым
            if (date.dayOfWeek == targetDay) {
                // Затем проверяем, что день разрешен согласно режиму уведомлений
                if (isDayAllowedForMode(date.dayOfWeek, mode, routeId, overrideSelectedDays)) {
                    return date
                }
            }
            date = date.plusDays(1)
        }
        
        // Если не нашли за 4 недели - возвращаем исходную дату (edge case)
        return startDate
    }
    
    /**
     * Конвертирует день недели между форматами
     * 
     * В Android Calendar: 1=воскресенье, 2=понедельник, ..., 7=суббота
     * В Java Time: 1=понедельник, 2=вторник, ..., 7=воскресенье
     * 
     * Эта функция переводит из Android формата в Java Time формат.
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
                DayOfWeek.MONDAY
            }
        }
    }
    
    /**
     * Находит следующий день недели
     * 
     * Ищет ближайший день, который соответствует указанному дню недели.
     * Если сегодня уже этот день, возвращает сегодня.
     * 
     * Пример: ищем понедельник, сегодня среда → возвращает следующий понедельник
     */
    private fun findNextOccurrenceForDayOfWeek(
        startDate: LocalDate,
        targetDayOfWeek: DayOfWeek
    ): LocalDate {
        var currentDate = startDate
        var attempts = 0
        val maxAttempts = 14 // 2 недели
        
        while (attempts < maxAttempts) {
            if (currentDate.dayOfWeek == targetDayOfWeek) {
                return currentDate
            }
            currentDate = currentDate.plusDays(1)
            attempts++
        }
        
        return startDate.plusDays(7) // Fallback: через неделю
    }
    
    /**
     * Находит следующий день недели для режима SELECTED_DAYS
     * 
     * Ищет ближайший день, который соответствует указанному дню недели
     * И который также входит в выбранные дни для уведомлений.
     * 
     * @param startDate дата начала поиска
     * @param targetDayOfWeek целевой день недели
     * @param routeId ID маршрута для получения настроек
     * @param overrideSelectedDays если заданы, используются вместо значений из кэша
     * @return дата следующего подходящего дня
     */
    private fun findNextOccurrenceForSelectedDays(
        startDate: LocalDate,
        targetDayOfWeek: DayOfWeek,
        routeId: String,
        overrideSelectedDays: Set<DayOfWeek>? = null
    ): LocalDate {
        val selectedDays = overrideSelectedDays ?: NotificationPreferencesCache.getSelectedDays(routeId)
        
        Timber.d("findNextOccurrenceForSelectedDays: startDate = $startDate, targetDayOfWeek = $targetDayOfWeek")
        Timber.d("findNextOccurrenceForSelectedDays: selectedDays = $selectedDays")
        
        var currentDate = startDate
        var attempts = 0
        val maxAttempts = 14 // 2 недели
        
        while (attempts < maxAttempts) {
            val isTargetDay = currentDate.dayOfWeek == targetDayOfWeek
            val isSelectedDay = currentDate.dayOfWeek in selectedDays
            
            Timber.d("findNextOccurrenceForSelectedDays: checking $currentDate (${currentDate.dayOfWeek}) - isTargetDay: $isTargetDay, isSelectedDay: $isSelectedDay")
            
            if (isTargetDay && isSelectedDay) {
                Timber.d("findNextOccurrenceForSelectedDays: found next occurrence: $currentDate")
                return currentDate
            }
            currentDate = currentDate.plusDays(1)
            attempts++
        }
        
        Timber.w("findNextOccurrenceForSelectedDays: not found in $maxAttempts attempts, using fallback")
        return startDate.plusDays(7) // Fallback: через неделю
    }
}