package com.example.lets_go_slavgorod.domain.notification

import timber.log.Timber
import java.time.DayOfWeek
import java.util.Calendar

/**
 * Стратегии для вычисления времени отправления
 * 
 * Разные способы вычисления времени в зависимости от режима уведомлений.
 * Каждая стратегия знает, как найти следующее время отправления для своего режима.
 * 
 * Зачем нужно:
 * - Код стал проще и понятнее
 * - Легко тестировать каждую стратегию отдельно
 * - Легко добавлять новые режимы
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
 * Стратегия "Все дни"
 * 
 * Планирует уведомления каждый день в указанное время.
 * Если время уже прошло сегодня, планирует на завтра.
 */
class AllDaysStrategy : DepartureTimeStrategy() {
    override fun calculateNextTime(baseTime: Calendar, now: Calendar): Long {
        val nextDeparture = (baseTime.clone() as Calendar).apply {
            // ИСПРАВЛЕНО: Планируем на сегодня, если время еще не прошло
            // Если время уже прошло сегодня, планируем на завтра
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            } else {
            }
        }
        
        return nextDeparture.timeInMillis
    }
}

/**
 * Стратегия "Только будни"
 * 
 * Планирует уведомления только в будние дни (понедельник-пятница).
 * Пропускает выходные и ищет следующий будний день.
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
                return candidateDeparture.timeInMillis
            }
        }
        
        Timber.e("Не удалось найти подходящий будний день в течение 2 недель")
        return -1L
    }
}

/**
 * Стратегия "Выбранные дни"
 * 
 * Планирует уведомления только в дни, которые выбрал пользователь.
 * 
 * Пример:
 * - Избранное: Понедельник 10:00
 * - Выбранные дни: [Понедельник, Среда, Пятница]
 * - Результат: Уведомления в Понедельник, Среду и Пятницу в 10:00
 * 
 * @param selectedDays дни недели, которые выбрал пользователь
 * @param targetDayOfWeek день недели избранного времени (null = каждый день)
 */
class SelectedDaysStrategy(
    private val selectedDays: Set<DayOfWeek>,
    private val targetDayOfWeek: DayOfWeek? = null
) : DepartureTimeStrategy() {
    
    override fun calculateNextTime(baseTime: Calendar, now: Calendar): Long {
        if (selectedDays.isEmpty()) {
            return -1L
        }
        
        // ВАЖНО: Если день недели не установлен (null), то планируем на любой выбранный день
        if (targetDayOfWeek == null) {
            
            // Ищем любой ближайший выбранный день
            for (i in 0..14) { // 2 недели
                val candidateDeparture = (baseTime.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, i)
                }
                
                val candidateDay = candidateDeparture.get(Calendar.DAY_OF_WEEK)
                val candidateDayOfWeek = mapCalendarDayToDayOfWeek(candidateDay)
                val isSelectedDay = candidateDayOfWeek != null && candidateDayOfWeek in selectedDays
                
                if (isSelectedDay && candidateDeparture.after(now)) {
                    return candidateDeparture.timeInMillis
                }
            }
        } else {
            // День недели установлен - проверяем, что он входит в выбранные дни
            if (targetDayOfWeek !in selectedDays) {
                return -1L
            }
            
            
            // Ищем ближайший день, который соответствует дню недели избранного времени
            for (i in 0..14) { // 2 недели
                val candidateDeparture = (baseTime.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_YEAR, i)
                }
                
                val candidateDay = candidateDeparture.get(Calendar.DAY_OF_WEEK)
                val candidateDayOfWeek = mapCalendarDayToDayOfWeek(candidateDay)
                val isSelectedDay = candidateDayOfWeek != null && candidateDayOfWeek in selectedDays
                
                if (isSelectedDay && candidateDeparture.after(now)) {
                    return candidateDeparture.timeInMillis
                }
            }
        }
        
        Timber.e("Не удалось найти выбранный день в течение 2 недель")
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
 * Стратегия "Отключено"
 * 
 * Уведомления отключены, поэтому всегда возвращает -1.
 * Нужна для единообразия API.
 */
class DisabledStrategy : DepartureTimeStrategy() {
    override fun calculateNextTime(baseTime: Calendar, now: Calendar): Long {
        return -1L
    }
}