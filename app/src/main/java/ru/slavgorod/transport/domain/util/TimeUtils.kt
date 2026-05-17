package ru.slavgorod.transport.domain.util

import java.util.Calendar
import java.util.Locale

object TimeUtils {

    fun formatTime(calendar: Calendar): String {
        return String.format(
            Locale.ROOT,
            "%02d:%02d",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE)
        )
    }

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

    fun isWeekend(calendar: Calendar): Boolean {
        return when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY, Calendar.SUNDAY -> true
            else -> false
        }
    }

    fun getMinutesUntil(currentTime: Calendar, targetTime: Calendar): Int {
        return targetTime.minutesOfDay() - currentTime.minutesOfDay()
    }

    private fun Calendar.minutesOfDay(): Int {
        return get(Calendar.HOUR_OF_DAY) * 60 + get(Calendar.MINUTE)
    }
}
