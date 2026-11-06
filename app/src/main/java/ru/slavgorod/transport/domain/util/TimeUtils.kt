package ru.slavgorod.transport.domain.util

import java.util.Calendar
import java.util.Locale

/** Операции с локальными значениями [Calendar], используемые в интерфейсе. */
object TimeUtils {

    /** Возвращает часы и минуты в формате с ведущими нулями. */
    fun formatTime(calendar: Calendar): String {
        return String.format(
            Locale.ROOT,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

    /** Преобразует дни Calendar к нумерации от понедельника (1) до воскресенья (7). */
    fun getDayOfWeek(calendar: Calendar): Int {
        return when (val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> dayOfWeek
        }
    }

    /** Считает выходными субботу и воскресенье без учёта праздничного календаря. */
    fun isWeekend(calendar: Calendar): Boolean {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY, Calendar.SUNDAY -> true
            else -> false
        }
    }

    /**
     * Вычитает время суток в минутах, не учитывая дату и переход через полночь.
     * Например, для 23:55 и 00:10 результат отрицателен; для поиска следующего
     * отправления через границу суток нужна логика с полноценной датой.
     *
     * @param currentTime Исходное время в часовом поясе переданного календаря.
     * @param targetTime Целевое время; используются только часы и минуты.
     * @return Разница минут без корректировки на следующий день.
     */
    fun getMinutesUntil(currentTime: Calendar, targetTime: Calendar): Int {
        return targetTime.minutesOfDay() - currentTime.minutesOfDay()
    }

    private fun Calendar.minutesOfDay(): Int {
        return get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
    }
}
