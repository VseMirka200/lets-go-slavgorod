package ru.slavgorod.transport.ui.components.schedule

import java.util.Calendar
import kotlin.math.abs

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
