package ru.slavgorod.transport.ui.components.schedule

import java.util.Calendar

/**
 * Форматирует длительность таймера; отрицательное значение отображается как ноль.
 */
internal fun buildScheduleTimerLabel(durationMillis: Long): String {
    val totalSeconds = (durationMillis.coerceAtLeast(0L) / 1_000L).toInt()
    val hours = totalSeconds / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60
    return buildString {
        if (hours > 0) append("${hours}ч ")
        append("${minutes}м ${seconds.toString().padStart(2, '0')}с")
    }
}

/** Ищет следующее отправление по времени суток, перенося близкий рейс на завтра. */
internal fun resolveDepartureMillis(
    departureTime: String,
    currentTimeMillis: Long
): Long? {
    val calendar = departureCalendar(departureTime, currentTimeMillis) ?: return null

    if (calendar.timeInMillis - currentTimeMillis <= MINIMUM_VISIBLE_COUNTDOWN_MILLIS) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    return calendar.timeInMillis
}

/** Возвращает время с последнего отправления, учитывая переход через полночь. */
internal fun resolveElapsedDepartureMillis(
    departureTime: String,
    currentTimeMillis: Long
): Long? {
    val departure = departureCalendar(departureTime, currentTimeMillis) ?: return null
    if (departure.timeInMillis > currentTimeMillis) departure.add(Calendar.DAY_OF_YEAR, -1)
    return currentTimeMillis - departure.timeInMillis
}

private fun departureCalendar(departureTime: String, currentTimeMillis: Long): Calendar? {
    val parts = departureTime.split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    return Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private const val MINIMUM_VISIBLE_COUNTDOWN_MILLIS = 60_000L
