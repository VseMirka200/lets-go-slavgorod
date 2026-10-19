package ru.slavgorod.transport.ui.components.schedule

import java.util.Calendar
import kotlin.math.abs

/**
 * Вспомогательные расчёты времени для карточек расписания.
 *
 * Подсказка разработчику: граница ближайшего рейса учитывает сутки, поэтому не
 * заменяйте Calendar простым вычитанием времени без обработки перехода через полночь.
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

internal fun resolveDepartureMillis(
    departureTime: String,
    currentTimeMillis: Long
): Long? {
    val parts = departureTime.split(":")
    if (parts.size != 2) return null

    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null

    val calendar = Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    if (calendar.timeInMillis - currentTimeMillis <= MINIMUM_VISIBLE_COUNTDOWN_MILLIS) {
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }

    return calendar.timeInMillis
}

internal fun resolveDepartureDiffMinutes(
    departureTime: String,
    currentTimeMillis: Long
): Int? {
    return resolveDepartureMillis(departureTime, currentTimeMillis)
        ?.let { departureMillis -> ((departureMillis - currentTimeMillis) / 60_000L).toInt() }
}

internal fun resolveElapsedDepartureMillis(
    departureTime: String,
    currentTimeMillis: Long
): Long? {
    val parts = departureTime.split(":")
    if (parts.size != 2) return null

    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    val departure = Calendar.getInstance().apply {
        timeInMillis = currentTimeMillis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        if (timeInMillis > currentTimeMillis) add(Calendar.DAY_OF_YEAR, -1)
    }
    return currentTimeMillis - departure.timeInMillis
}

/** Возвращает минуты, прошедшие с последнего наступления времени отправления. */
internal fun resolveElapsedDepartureMinutes(
    departureTime: String,
    currentTimeMillis: Long
): Int? {
    return resolveElapsedDepartureMillis(departureTime, currentTimeMillis)
        ?.let { (it / 60_000L).toInt() }
}

internal fun parseTravelDurationMinutes(value: String?): Int? {
    val text = value.orEmpty().lowercase()
    if (text.isBlank()) return null

    val hours = Regex("(\\d+)\\s*(?:ч|час)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val minutes = Regex("(\\d+)\\s*(?:м(?![а-я]))|(?:\\d+)\\s*мин")
        .find(text)
        ?.value
        ?.filter(Char::isDigit)
        ?.toIntOrNull()
        ?: if (hours == 0) Regex("\\d+").find(text)?.value?.toIntOrNull() ?: 0 else 0
    return (hours * 60 + minutes).takeIf { it > 0 }
}

internal fun buildCountdownLabelArgs(diffMinutes: Int): CountdownLabelArgs {
    return when {
        diffMinutes < 0 -> CountdownLabelArgs.AlreadyDeparted
        diffMinutes < 60 -> CountdownLabelArgs.Minutes(diffMinutes)
        diffMinutes % 60 == 0 -> CountdownLabelArgs.Hours(diffMinutes / 60)
        else -> CountdownLabelArgs.HoursMinutes(
            hours = diffMinutes / 60,
            minutes = abs(diffMinutes % 60).toString().padStart(2, '0')
        )
    }
}

internal sealed interface CountdownLabelArgs {
    data object AlreadyDeparted : CountdownLabelArgs
    data class Minutes(val minutes: Int) : CountdownLabelArgs
    data class Hours(val hours: Int) : CountdownLabelArgs
    data class HoursMinutes(val hours: Int, val minutes: String) : CountdownLabelArgs
}

private const val MINIMUM_VISIBLE_COUNTDOWN_MILLIS = 60_000L
