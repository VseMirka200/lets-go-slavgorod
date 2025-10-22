package com.example.lets_go_slavgorod.domain.notification

import timber.log.Timber
import java.time.DayOfWeek
import java.util.Calendar

/**
 * Стратегия вычисления следующего времени отправления
 * 
 * Паттерн Strategy для разделения сложной логики вычисления времени отправления
 * на отдельные стратегии для каждого режима уведомлений.
 * 
 * Преимущества:
 * - Упрощение AlarmScheduler (снижение CC с 18 до 5)
 * - Легко тестировать каждую стратегию отдельно
 * - Легко добавлять новые режимы уведомлений
 * - Соответствие Open/Closed Principle
 * 
 * @author VseMirka200
 * @version 1.0
 * @since 2.1
 */
sealed class DepartureTimeStrategy {
    
    /**
     * Вычисляет следующее время отправления в миллисекундах
     * 
     * @param baseTime базовое время отправления (уже установлено HH:mm)
     * @param now текущее время
     * @return timestamp следующего отправления или -1 при ошибке
     */
    abstract fun calculateNextTime(baseTime: Calendar, now: Calendar): Long
    
    /**
     * Проверяет, находится ли время в будущем
     */
    protected fun isInFuture(time: Calendar, now: Calendar): Boolean {
        return time.after(now)
    }
    
    /**
     * Форматирует для логирования
     */
    protected fun formatForLog(calendar: Calendar): String {
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH)+1}-${calendar.get(Calendar.DAY_OF_MONTH)} ${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}"
    }
}

/**
 * Стратегия для режима ALL_DAYS - каждый день
 */
class AllDaysStrategy : DepartureTimeStrategy() {
    override fun calculateNextTime(baseTime: Calendar, now: Calendar): Long {
        val nextDeparture = (baseTime.clone() as Calendar).apply {
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        
        Timber.d("AllDaysStrategy: Next departure at ${formatForLog(nextDeparture)}")
        return nextDeparture.timeInMillis
    }
}

/**
 * Стратегия для режима WEEKDAYS - только будние дни (пн-пт)
 */
class WeekdaysStrategy : DepartureTimeStrategy() {
    override fun calculateNextTime(baseTime: Calendar, now: Calendar): Long {
        // Проверяем 2 недели вперед
        for (i in 0..14) {
            val candidateDeparture = (baseTime.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            
            val candidateDay = candidateDeparture.get(Calendar.DAY_OF_WEEK)
            val isWeekday = candidateDay in Calendar.MONDAY..Calendar.FRIDAY
            
            if (isWeekday && candidateDeparture.after(now)) {
                Timber.d("WeekdaysStrategy: Next departure at ${formatForLog(candidateDeparture)} (day $candidateDay)")
                return candidateDeparture.timeInMillis
            }
        }
        
        Timber.e("WeekdaysStrategy: Could not find suitable weekday within 2 weeks")
        return -1L
    }
}

/**
 * Стратегия для режима SELECTED_DAYS - выбранные дни недели
 * 
 * ИСПРАВЛЕНО v2.2: Теперь работает согласованно с ALL_DAYS/WEEKDAYS
 * Игнорирует день недели избранного времени и ищет ЛЮБОЙ ближайший выбранный день.
 * 
 * Пример:
 * - Избранное: Понедельник 10:00
 * - Выбранные дни: [Среда, Пятница]
 * - Результат: Уведомления в Среду и Пятницу в 10:00 ✅
 * 
 * @param selectedDays набор выбранных дней недели для уведомлений
 * @param targetDayOfWeek (устаревший параметр, игнорируется)
 */
class SelectedDaysStrategy(
    private val selectedDays: Set<DayOfWeek>,
    @Suppress("UNUSED_PARAMETER")
    private val targetDayOfWeek: DayOfWeek? = null
) : DepartureTimeStrategy() {
    
    override fun calculateNextTime(baseTime: Calendar, now: Calendar): Long {
        if (selectedDays.isEmpty()) {
            Timber.w("SelectedDaysStrategy: No days selected")
            return -1L
        }
        
        // В режиме "Выбранные дни" игнорируем день недели избранного времени
        // Уведомления должны приходить в ЛЮБОЙ выбранный день, не только в день избранного
        // Это согласуется с поведением ALL_DAYS и WEEKDAYS
        
        Timber.d("SelectedDaysStrategy: Searching for next selected day. Selected days: $selectedDays")
        
        // Ищем ЛЮБОЙ ближайший выбранный день
        for (i in 0..14) { // 2 недели
            val candidateDeparture = (baseTime.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, i)
            }
            
            val candidateDay = candidateDeparture.get(Calendar.DAY_OF_WEEK)
            val candidateDayOfWeek = mapCalendarDayToDayOfWeek(candidateDay)
            val isSelectedDay = candidateDayOfWeek != null && candidateDayOfWeek in selectedDays
            
            if (isSelectedDay && candidateDeparture.after(now)) {
                Timber.d("SelectedDaysStrategy: Next departure at ${formatForLog(candidateDeparture)} ($candidateDayOfWeek)")
                return candidateDeparture.timeInMillis
            }
        }
        
        Timber.e("SelectedDaysStrategy: Could not find selected day within 2 weeks. Selected: $selectedDays")
        return -1L
    }
    
    /**
     * Конвертирует Calendar.DAY_OF_WEEK в java.time.DayOfWeek
     */
    private fun mapCalendarDayToDayOfWeek(calendarDay: Int): DayOfWeek? {
        return when (calendarDay) {
            Calendar.SUNDAY -> DayOfWeek.SUNDAY
            Calendar.MONDAY -> DayOfWeek.MONDAY
            Calendar.TUESDAY -> DayOfWeek.TUESDAY
            Calendar.WEDNESDAY -> DayOfWeek.WEDNESDAY
            Calendar.THURSDAY -> DayOfWeek.THURSDAY
            Calendar.FRIDAY -> DayOfWeek.FRIDAY
            Calendar.SATURDAY -> DayOfWeek.SATURDAY
            else -> null
        }
    }
}

/**
 * Стратегия для режима DISABLED - уведомления отключены
 */
class DisabledStrategy : DepartureTimeStrategy() {
    override fun calculateNextTime(baseTime: Calendar, now: Calendar): Long {
        Timber.d("DisabledStrategy: Notifications disabled")
        return -1L
    }
}